package au.edu.group2.disp.workers;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Phase-1 workers — the first four service tasks agreed as the foundation scope.
 *
 * <p>These cover the two main order-capture paths (online and in-person) plus
 * the finance-request handoff to FinTrust and FinTrust's bill-payment callback.
 *
 * <p><b>BPMN pools served:</b>
 * <ul>
 *   <li>Probuild main process ({@code Pro}) — processOrder, processOrderInPerson, financeRequest</li>
 *   <li>FinTrust process ({@code Process_1kwzimz}) — payBill</li>
 * </ul>
 *
 * <p><b>What is a @Component?</b><br>
 * {@code @Component} tells Spring Boot to automatically create one instance of this
 * class when the application starts. You never call {@code new FoundationWorkers()}
 * yourself — Spring manages it.
 *
 * <p><b>What is a @JobWorker?</b><br>
 * {@code @JobWorker(type = "...")} registers the method as a handler for a specific
 * Camunda service-task type. When a process instance reaches a service task whose
 * {@code zeebe:taskDefinition type} matches, Camunda pushes the job to this method.
 * The method runs, returns a {@code Map} of output variables, and Camunda advances
 * the token to the next step automatically.
 */
@Component
public class FoundationWorkers {

    private static final Logger LOGGER = LoggerFactory.getLogger(FoundationWorkers.class);

    /**
     * Captures an online order placed through the ProBuild website or app.
     *
     * <p><b>Business context:</b> When a customer completes checkout online,
     * the BPMN process reaches this service task. The worker records the order,
     * generates a unique order ID if one has not already been assigned, and
     * marks the order as received so downstream tasks (payment, fulfilment) can proceed.
     *
     * <p><b>BPMN task name:</b> "get order info"<br>
     * <b>Task type:</b> {@code processOrder}
     *
     * @param job  the activated Camunda job — carries all process variables set so far
     * @return map of output variables written back into the process instance:
     *         <ul>
     *           <li>{@code orderId} — unique order reference (e.g. "ORD-A3F2B1C4")</li>
     *           <li>{@code orderCaptured} — always {@code true}; confirms capture succeeded</li>
     *           <li>{@code orderCapturedAt} — ISO-8601 timestamp of capture</li>
     *           <li>{@code orderStatus} — set to "RECEIVED" to drive downstream gateways</li>
     *           <li>{@code orderChannel} — echoes back the channel ("online" if not provided)</li>
     *         </ul>
     */
    @JobWorker(type = "processOrder")
    public Map<String, Object> processOrder(ActivatedJob job) {
        Map<String, Object> vars = safeVars(job);
        LOGGER.info("processOrder: jobKey={}, processInstanceKey={}", job.getKey(), job.getProcessInstanceKey());

        // Re-use an orderId if one was already set earlier in the process (e.g. from a
        // shopping-cart system), otherwise generate a fresh one.
        String orderId = (String) vars.get("orderId");
        if (orderId == null || orderId.isBlank()) {
            orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        LOGGER.info("processOrder: orderId={}, customerId={}, orderTotal={}",
                orderId, vars.get("customerId"), vars.getOrDefault("orderTotal", 0));

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("orderCaptured", true);
        result.put("orderCapturedAt", Instant.now().toString());
        result.put("orderStatus", "RECEIVED");
        result.put("orderChannel", vars.getOrDefault("orderChannel", "online"));
        return result;
    }

    /**
     * Captures an order placed by a customer standing at the ProBuild counter.
     *
     * <p><b>Business context:</b> A staff member has verified the customer's
     * credentials (see {@code credentialCheck}) and processed their selections.
     * This task creates the order record for an in-person transaction, mirroring
     * what {@code processOrder} does for online sales.
     *
     * <p><b>BPMN task name:</b> "get order info (in-person)" / "process order (in-person)"<br>
     * <b>Task type:</b> {@code processOrderInPerson}
     *
     * @param job the activated Camunda job
     * @return map of output variables:
     *         <ul>
     *           <li>{@code orderId} — unique in-person order reference (prefixed "ORD-IP-")</li>
     *           <li>{@code inPersonOrderCaptured} — always {@code true}</li>
     *           <li>{@code orderCapturedAt} — ISO-8601 timestamp</li>
     *           <li>{@code orderStatus} — "RECEIVED"</li>
     *           <li>{@code orderChannel} — hardcoded "inPerson"</li>
     *         </ul>
     */
    @JobWorker(type = "processOrderInPerson")
    public Map<String, Object> processOrderInPerson(ActivatedJob job) {
        Map<String, Object> vars = safeVars(job);
        LOGGER.info("processOrderInPerson: jobKey={}, processInstanceKey={}", job.getKey(), job.getProcessInstanceKey());

        String orderId = "ORD-IP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LOGGER.info("processOrderInPerson: customerId={}, orderId={}", vars.get("customerId"), orderId);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("inPersonOrderCaptured", true);
        result.put("orderCapturedAt", Instant.now().toString());
        result.put("orderStatus", "RECEIVED");
        result.put("orderChannel", "inPerson");
        return result;
    }

    /**
     * Sends a finance request to FinTrust on behalf of a customer who has chosen
     * to pay in instalments rather than up front.
     *
     * <p><b>Business context:</b> ProBuild offers 6-month and 12-month instalment
     * plans through its finance partner FinTrust. When a customer selects finance at
     * checkout, this task records the request and decides whether it can be
     * auto-approved.
     *
     * <p><b>Business rule — auto-approval threshold:</b><br>
     * Orders up to and including £10,000 are automatically approved. Orders above
     * £10,000 are flagged for manual review by a finance manager before FinTrust
     * is engaged.
     *
     * <p><b>BPMN task name:</b> "request payment from FinTrust"<br>
     * <b>Task type:</b> {@code financeRequest}
     *
     * @param job the activated Camunda job. Expected process variables:
     *            <ul>
     *              <li>{@code orderTotal} — total order value in GBP</li>
     *              <li>{@code customerId} — customer identifier</li>
     *              <li>{@code financeInstallments} — 6 or 12 (defaults to 12)</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code financeRequestId} — unique reference for this finance application</li>
     *           <li>{@code financeApproved} — {@code true} if order ≤ £10,000</li>
     *           <li>{@code financeReviewedAt} — ISO-8601 timestamp</li>
     *           <li>{@code financeDecisionReason} — plain-English explanation of the decision</li>
     *           <li>{@code financeInstallments} — echoed back for use by FinTrust tasks</li>
     *         </ul>
     */
    @JobWorker(type = "financeRequest")
    public Map<String, Object> financeRequest(ActivatedJob job) {
        Map<String, Object> vars = safeVars(job);
        LOGGER.info("financeRequest: jobKey={}, processInstanceKey={}", job.getKey(), job.getProcessInstanceKey());

        Number orderTotal = (Number) vars.getOrDefault("orderTotal", 0);
        Number installments = (Number) vars.getOrDefault("financeInstallments", 12);

        // Auto-approve orders up to £10,000 — anything larger needs a human decision.
        boolean approved = orderTotal.doubleValue() <= 10_000.0;
        String financeRequestId = "FIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        LOGGER.info("financeRequest: customerId={}, orderTotal={}, installments={}, approved={}",
                vars.get("customerId"), orderTotal, installments, approved);

        Map<String, Object> result = new HashMap<>();
        result.put("financeRequestId", financeRequestId);
        result.put("financeApproved", approved);
        result.put("financeReviewedAt", Instant.now().toString());
        result.put("financeDecisionReason", approved
                ? "Auto-approved: within £10,000 threshold"
                : "Referred for manual review: order total exceeds £10,000");
        result.put("financeInstallments", installments);
        return result;
    }

    /**
     * FinTrust pays the outstanding bill to ProBuild on behalf of the customer.
     *
     * <p><b>Business context:</b> This task runs inside the FinTrust pool. Once
     * FinTrust has received a finance request from ProBuild (via a message event),
     * it processes the payment — effectively settling the invoice with ProBuild
     * immediately while FinTrust then recovers the money from the customer in
     * monthly instalments.
     *
     * <p><b>BPMN task name:</b> "pay customer bill"<br>
     * <b>Task type:</b> {@code payBill}
     *
     * @param job the activated Camunda job. Expected process variables:
     *            <ul>
     *              <li>{@code orderTotal} — amount FinTrust is settling</li>
     *              <li>{@code customerId} — customer on whose behalf payment is made</li>
     *              <li>{@code financeRequestId} — links this payment to the finance application</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code paymentStatus} — "PAID"</li>
     *           <li>{@code paymentProcessedAt} — ISO-8601 timestamp</li>
     *           <li>{@code transactionId} — unique payment reference (prefixed "BILL-")</li>
     *           <li>{@code financeConfirmation} — {@code true}; triggers the message catch
     *               event back in the ProBuild process</li>
     *         </ul>
     */
    @JobWorker(type = "payBill")
    public Map<String, Object> payBill(ActivatedJob job) {
        Map<String, Object> vars = safeVars(job);
        LOGGER.info("payBill: jobKey={}, processInstanceKey={}", job.getKey(), job.getProcessInstanceKey());

        String transactionId = "BILL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LOGGER.info("payBill: customerId={}, orderTotal={}, financeRequestId={}",
                vars.get("customerId"), vars.getOrDefault("orderTotal", 0), vars.get("financeRequestId"));

        Map<String, Object> result = new HashMap<>();
        result.put("paymentStatus", "PAID");
        result.put("paymentProcessedAt", Instant.now().toString());
        result.put("transactionId", transactionId);
        // Setting financeConfirmation = true allows the conditional start event
        // in the FinTrust process to trigger the instalment collection sub-process.
        result.put("financeConfirmation", true);
        return result;
    }

    /**
     * Safely retrieves the process variables map from a job.
     *
     * <p>Camunda can theoretically return {@code null} for the variables map if
     * no variables have been set yet on the instance. This helper ensures we always
     * get an empty map rather than a NullPointerException when calling
     * {@code vars.getOrDefault(...)}.
     *
     * <p>Package-private so sibling worker classes can reuse it without duplicating
     * the null-check everywhere.
     *
     * @param job the activated job
     * @return the variables map, or an empty map if none exist
     */
    static Map<String, Object> safeVars(ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        return vars == null ? Map.of() : vars;
    }
}
