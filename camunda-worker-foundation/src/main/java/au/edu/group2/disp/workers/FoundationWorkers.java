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

@Component
public class FoundationWorkers {

    private static final Logger LOGGER = LoggerFactory.getLogger(FoundationWorkers.class);

    // Inputs: customerId, items, orderTotal, orderChannel
    // Outputs: orderId, orderCaptured, orderCapturedAt, orderStatus, orderChannel
    @JobWorker(type = "processOrder")
    public Map<String, Object> processOrder(ActivatedJob job) {
        Map<String, Object> vars = safeVars(job);
        LOGGER.info("processOrder: jobKey={}, processInstanceKey={}", job.getKey(), job.getProcessInstanceKey());

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

    // Inputs: customerId, membershipId, items, orderTotal
    // Outputs: orderId, inPersonOrderCaptured, orderCapturedAt, orderStatus, orderChannel
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

    // Inputs: orderTotal, customerId, financeInstallments (6 or 12)
    // Outputs: financeRequestId, financeApproved, financeReviewedAt, financeDecisionReason
    // Rule: auto-approve orders up to £10,000; flag larger orders for manual review
    @JobWorker(type = "financeRequest")
    public Map<String, Object> financeRequest(ActivatedJob job) {
        Map<String, Object> vars = safeVars(job);
        LOGGER.info("financeRequest: jobKey={}, processInstanceKey={}", job.getKey(), job.getProcessInstanceKey());

        Number orderTotal = (Number) vars.getOrDefault("orderTotal", 0);
        Number installments = (Number) vars.getOrDefault("financeInstallments", 12);
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

    // Inputs: orderTotal, customerId, financeRequestId
    // Outputs: paymentStatus, paymentProcessedAt, transactionId, financeConfirmation
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
        result.put("financeConfirmation", true);
        return result;
    }

    static Map<String, Object> safeVars(ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        return vars == null ? Map.of() : vars;
    }
}
