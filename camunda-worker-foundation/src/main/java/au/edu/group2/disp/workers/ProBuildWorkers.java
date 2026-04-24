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
 * Workers for the main <b>Probuild process</b> ({@code Pro}) covering in-store
 * payment, membership management, customer credential verification, and queue
 * handling.
 *
 * <p>This class groups tasks that sit inside the ProBuild pool but are not
 * specifically about inventory or tool rental — those live in
 * {@link InventoryWorkers} and {@link ToolRentalWorkers} respectively.
 *
 * <p><b>Key process variables used across these workers:</b>
 * <ul>
 *   <li>{@code customerId} — unique customer identifier, set at the start of the process</li>
 *   <li>{@code orderTotal} — running total in GBP, updated by {@code calculate-order-amount}</li>
 *   <li>{@code paymentSuccessful} — "yes" or "no", read by the gateway after card payment</li>
 *   <li>{@code membershipType} — "standard", "trade", or "premium"</li>
 * </ul>
 */
@Component
public class ProBuildWorkers {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProBuildWorkers.class);

    /**
     * Calculates the total amount due for an order or rental.
     *
     * <p><b>Business context:</b> Before the customer pays, the system needs to
     * know exactly how much to charge. For rentals this is unit price × number
     * of days × quantity. For outright purchases {@code rentalDays} defaults to 1.
     * The result is stored as {@code orderTotal} so the downstream payment tasks
     * know what to charge.
     *
     * <p><b>Task type:</b> {@code calculate-order-amount}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code unitPrice} — price per unit per day (or per item for sales)</li>
     *              <li>{@code rentalDays} — number of days hired (default 1 for purchases)</li>
     *              <li>{@code quantity} — number of units (default 1)</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code orderTotal} — {@code unitPrice × rentalDays × quantity}</li>
     *           <li>{@code itemCount} — quantity as an integer</li>
     *         </ul>
     */
    @JobWorker(type = "calculate-order-amount")
    public Map<String, Object> calculateOrderAmount(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        Number unitPrice = (Number) vars.getOrDefault("unitPrice", 0);
        Number rentalDays = (Number) vars.getOrDefault("rentalDays", 1);
        Number quantity = (Number) vars.getOrDefault("quantity", 1);
        LOGGER.info("calculate-order-amount: unitPrice={}, rentalDays={}, quantity={}",
                unitPrice, rentalDays, quantity);

        double orderTotal = unitPrice.doubleValue() * rentalDays.doubleValue() * quantity.doubleValue();

        Map<String, Object> result = new HashMap<>();
        result.put("orderTotal", orderTotal);
        result.put("itemCount", quantity.intValue());
        return result;
    }

    /**
     * Processes a card payment at the in-store or online ProBuild checkout.
     *
     * <p><b>Business context:</b> Called when the customer selects "card" at the
     * payment-method gateway. The worker submits the card to the payment processor
     * and records whether it was authorised.
     *
     * <p><b>Critical output — {@code paymentSuccessful}:</b><br>
     * The BPMN exclusive gateway immediately after this task reads
     * {@code =paymentSuccessful = "yes"} and {@code =paymentSuccessful = "no"}.
     * The value <em>must</em> be the string "yes" or "no" — not a boolean — because
     * that is how the condition expressions are written in the BPMN.
     *
     * <p><b>Task type:</b> {@code receive-card-payment}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code orderTotal} — amount to charge</li>
     *              <li>{@code cardToken} — tokenised card reference</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code paymentSuccessful} — <b>"yes"</b> or "no" (string, not boolean)</li>
     *           <li>{@code transactionId} — payment reference (prefixed "CARD-")</li>
     *           <li>{@code paymentProcessedAt} — ISO-8601 timestamp</li>
     *         </ul>
     */
    @JobWorker(type = "receive-card-payment")
    public Map<String, Object> receiveCardPayment(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        Number orderTotal = (Number) vars.getOrDefault("orderTotal", 0);
        LOGGER.info("receive-card-payment: orderTotal={}", orderTotal);

        Map<String, Object> result = new HashMap<>();
        // Must be the string "yes"/"no" — the BPMN gateway condition uses string comparison.
        result.put("paymentSuccessful", "yes");
        result.put("transactionId", "CARD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("paymentProcessedAt", Instant.now().toString());
        return result;
    }

    /**
     * Prints a receipt for the customer after a successful transaction.
     *
     * <p><b>Business context:</b> Triggered when {@code reciept = true} at the
     * receipt gateway. In a real integration this would send a print command to the
     * POS receipt printer or an email receipt to the customer. Currently it records
     * that the receipt was issued and generates a reference number.
     *
     * <p><b>Note on spelling:</b> The task type is {@code printReciept} (misspelled
     * "receipt") because that is exactly how it appears in the BPMN
     * {@code zeebe:taskDefinition}. The type string must match the BPMN exactly.
     *
     * <p><b>Task type:</b> {@code printReciept}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code orderId} — the order this receipt relates to</li>
     *              <li>{@code orderTotal} — total shown on the receipt</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code receiptPrinted} — {@code true}</li>
     *           <li>{@code receiptReference} — unique receipt number (prefixed "RCP-")</li>
     *           <li>{@code receiptPrintedAt} — ISO-8601 timestamp</li>
     *         </ul>
     */
    @JobWorker(type = "printReciept")
    public Map<String, Object> printReceipt(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String orderId = (String) vars.getOrDefault("orderId", "UNKNOWN");
        LOGGER.info("printReciept: orderId={}", orderId);

        Map<String, Object> result = new HashMap<>();
        result.put("receiptPrinted", true);
        result.put("receiptReference", "RCP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("receiptPrintedAt", Instant.now().toString());
        return result;
    }

    /**
     * Records a point-of-sale transaction for cash or card payments made at the counter.
     *
     * <p><b>Business context:</b> The POS system needs a transaction record regardless
     * of payment method. This task creates that record and assigns a POS transaction ID
     * used for end-of-day reconciliation.
     *
     * <p><b>Task type:</b> {@code POSTransaction}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code orderTotal} — final amount collected</li>
     *              <li>{@code paymentMethod} — "cash" or "card"</li>
     *              <li>{@code orderId} — the associated order</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code posTransactionId} — unique POS reference (prefixed "POS-")</li>
     *           <li>{@code transactionComplete} — {@code true}</li>
     *           <li>{@code posProcessedAt} — ISO-8601 timestamp</li>
     *         </ul>
     */
    @JobWorker(type = "POSTransaction")
    public Map<String, Object> posTransaction(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String paymentMethod = (String) vars.getOrDefault("paymentMethod", "cash");
        Number orderTotal = (Number) vars.getOrDefault("orderTotal", 0);
        LOGGER.info("POSTransaction: paymentMethod={}, orderTotal={}", paymentMethod, orderTotal);

        Map<String, Object> result = new HashMap<>();
        result.put("posTransactionId", "POS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("transactionComplete", true);
        result.put("posProcessedAt", Instant.now().toString());
        return result;
    }

    /**
     * Validates a customer's login credentials or membership card details.
     *
     * <p><b>Business context:</b> Used in two places in the BPMN:
     * <ol>
     *   <li>"Check if credentials are valid" — the first check when a customer
     *       presents at the counter.</li>
     *   <li>"Display failed inputs" — called when credentials fail, allowing
     *       the customer to correct and retry.</li>
     * </ol>
     * Both uses share the same job type because the underlying validation logic
     * is the same. Camunda will route jobs from both tasks to this worker.
     *
     * <p><b>Task type:</b> {@code credentialCheck}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code customerId} — customer identifier</li>
     *              <li>{@code memberEmail} — email address linked to the account</li>
     *              <li>{@code membershipId} — optional membership card number</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code credentialsValid} — {@code true} if the customer was found
     *               and details matched (stub: always {@code true})</li>
     *           <li>{@code membershipActive} — {@code true} if their membership is current</li>
     *         </ul>
     */
    @JobWorker(type = "credentialCheck")
    public Map<String, Object> credentialCheck(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String customerId = (String) vars.getOrDefault("customerId", "CUST-UNKNOWN");
        LOGGER.info("credentialCheck: customerId={}", customerId);

        Map<String, Object> result = new HashMap<>();
        result.put("credentialsValid", true);
        result.put("membershipActive", true);
        return result;
    }

    /**
     * Updates the customer's membership record in the ProBuild database.
     *
     * <p><b>Business context:</b> Called after a customer's trade or loyalty
     * membership has been validated and the order processed. It writes the
     * latest membership status, tier, and any renewed expiry date back to the
     * CRM so the record stays current.
     *
     * <p><b>Task type:</b> {@code updateMembership}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code customerId} — whose record to update</li>
     *              <li>{@code membershipType} — "standard", "trade", or "premium"</li>
     *              <li>{@code membershipId} — membership card number</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code membershipUpdated} — {@code true}</li>
     *           <li>{@code membershipUpdatedAt} — ISO-8601 timestamp</li>
     *         </ul>
     */
    @JobWorker(type = "updateMembership")
    public Map<String, Object> updateMembership(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String customerId = (String) vars.getOrDefault("customerId", "CUST-UNKNOWN");
        String membershipType = (String) vars.getOrDefault("membershipType", "standard");
        LOGGER.info("updateMembership: customerId={}, membershipType={}", customerId, membershipType);

        Map<String, Object> result = new HashMap<>();
        result.put("membershipUpdated", true);
        result.put("membershipUpdatedAt", Instant.now().toString());
        return result;
    }

    /**
     * Sends the customer their membership confirmation and arranges delivery
     * of their physical membership card.
     *
     * <p><b>Business context:</b> The final step in the new-membership flow.
     * Once the database has been updated, the customer receives a confirmation
     * email and their card is queued for printing and postal delivery.
     *
     * <p><b>Task type:</b> {@code membershipConfimed} (note: matches the BPMN spelling)</p>
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code customerId} — whose confirmation to send</li>
     *              <li>{@code customerEmail} — destination for the confirmation email</li>
     *              <li>{@code membershipId} — card number to include in the email</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code membershipConfirmed} — {@code true}</li>
     *           <li>{@code confirmationSentAt} — ISO-8601 timestamp</li>
     *         </ul>
     */
    @JobWorker(type = "membershipConfimed")
    public Map<String, Object> membershipConfirmed(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String customerId = (String) vars.getOrDefault("customerId", "CUST-UNKNOWN");
        LOGGER.info("membershipConfimed: customerId={}", customerId);

        Map<String, Object> result = new HashMap<>();
        result.put("membershipConfirmed", true);
        result.put("confirmationSentAt", Instant.now().toString());
        return result;
    }

    /**
     * Looks up a business in the trade account database to verify it holds a
     * valid ProBuild trade account.
     *
     * <p><b>Business context:</b> Trade customers (builders, contractors) get
     * preferential pricing. Before applying a trade discount, ProBuild verifies
     * the business's ABN (Australian Business Number) against its trade database
     * to confirm the account is active and what discount tier applies.
     *
     * <p><b>Task type:</b> {@code tradeDatabase}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code businessId} or {@code abn} — business identifier</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code tradeAccountActive} — {@code true} if account is in good standing</li>
     *           <li>{@code discountRate} — e.g. {@code 0.10} for 10% trade discount (stub)</li>
     *           <li>{@code tradeCategory} — account tier ("standard" in stub)</li>
     *         </ul>
     */
    @JobWorker(type = "tradeDatabase")
    public Map<String, Object> tradeDatabase(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String businessId = (String) vars.getOrDefault("businessId", vars.getOrDefault("abn", "UNKNOWN"));
        LOGGER.info("tradeDatabase: businessId={}", businessId);

        Map<String, Object> result = new HashMap<>();
        result.put("tradeAccountActive", true);
        result.put("discountRate", 0.10);
        result.put("tradeCategory", "standard");
        return result;
    }

    /**
     * Applies a membership discount to the order total.
     *
     * <p><b>Business context:</b> Members of ProBuild's loyalty scheme receive
     * a discount on their purchases. The discount rate depends on the membership
     * tier. This task calculates and applies the reduction before the customer
     * is asked to pay.
     *
     * <p><b>Discount rates (business rule):</b>
     * <ul>
     *   <li>premium — 15%</li>
     *   <li>trade — 10%</li>
     *   <li>standard — 5%</li>
     * </ul>
     *
     * <p><b>Task type:</b> {@code membershipDiscount}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code orderTotal} — pre-discount total in GBP</li>
     *              <li>{@code membershipType} — "standard", "trade", or "premium"</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code discountedTotal} — new total after discount</li>
     *           <li>{@code orderTotal} — overwritten with the discounted value so downstream
     *               payment tasks charge the right amount</li>
     *           <li>{@code discountApplied} — {@code true}</li>
     *           <li>{@code discountAmount} — how much was saved in GBP</li>
     *         </ul>
     */
    @JobWorker(type = "membershipDiscount")
    public Map<String, Object> membershipDiscount(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        Number orderTotal = (Number) vars.getOrDefault("orderTotal", 0);
        String membershipType = (String) vars.getOrDefault("membershipType", "standard");
        LOGGER.info("membershipDiscount: orderTotal={}, membershipType={}", orderTotal, membershipType);

        // Determine discount rate from membership tier.
        double rate = switch (membershipType) {
            case "premium" -> 0.15;
            case "trade"   -> 0.10;
            default        -> 0.05; // standard
        };
        double discountAmount = orderTotal.doubleValue() * rate;
        double discountedTotal = orderTotal.doubleValue() - discountAmount;

        Map<String, Object> result = new HashMap<>();
        result.put("discountedTotal", discountedTotal);
        result.put("discountApplied", true);
        result.put("discountAmount", discountAmount);
        // Overwrite orderTotal so payment tasks automatically use the reduced figure.
        result.put("orderTotal", discountedTotal);
        return result;
    }

    /**
     * Places the customer in a service queue and assigns their position.
     *
     * <p><b>Business context:</b> When a customer arrives in-store and all staff
     * are busy, they join a virtual queue. Members receive priority (position 1)
     * over non-members (position 5 in the stub) to reward loyalty.
     *
     * <p><b>Task type:</b> {@code Queue}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code customerId} — the customer joining the queue</li>
     *              <li>{@code isMember} — {@code true} if they hold a loyalty card</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code queuePosition} — their place in the queue (lower = sooner served)</li>
     *           <li>{@code estimatedWaitMinutes} — rough wait time (position × 5 minutes)</li>
     *         </ul>
     */
    @JobWorker(type = "Queue")
    public Map<String, Object> queue(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String customerId = (String) vars.getOrDefault("customerId", "CUST-UNKNOWN");
        boolean isMember = Boolean.parseBoolean(String.valueOf(vars.getOrDefault("isMember", false)));
        LOGGER.info("Queue: customerId={}, isMember={}", customerId, isMember);

        // Members jump to the front; non-members are placed further back.
        int position = isMember ? 1 : 5;

        Map<String, Object> result = new HashMap<>();
        result.put("queuePosition", position);
        result.put("estimatedWaitMinutes", position * 5);
        return result;
    }

    /**
     * Sends the customer an email confirming their place in the queue.
     *
     * <p><b>Business context:</b> Once queued, the customer receives an email
     * telling them their position and estimated wait so they do not need to
     * stand at the counter. The process then ends (the customer will be called
     * forward by staff when it is their turn).
     *
     * <p><b>Task type:</b> {@code queueEmail}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code customerEmail} — email address to notify</li>
     *              <li>{@code queuePosition} — their assigned position</li>
     *              <li>{@code estimatedWaitMinutes} — wait time to include in the email</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code emailSent} — {@code true}</li>
     *           <li>{@code emailSentAt} — ISO-8601 timestamp</li>
     *         </ul>
     */
    @JobWorker(type = "queueEmail")
    public Map<String, Object> queueEmail(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String customerEmail = (String) vars.getOrDefault("customerEmail", "unknown@example.com");
        Number queuePosition = (Number) vars.getOrDefault("queuePosition", 0);
        LOGGER.info("queueEmail: customerEmail={}, queuePosition={}", customerEmail, queuePosition);

        Map<String, Object> result = new HashMap<>();
        result.put("emailSent", true);
        result.put("emailSentAt", Instant.now().toString());
        return result;
    }

    /**
     * Sends the customer an email notifying them that their payment was declined.
     *
     * <p><b>Business context:</b> When a card payment fails and cannot be retried
     * (or the customer cancels), this task fires as a message end event to inform
     * the customer and suggest alternatives (e.g. try a different card, pay in store).
     *
     * <p><b>Task type:</b> {@code paymentDecline}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code customerId} — whose payment was declined</li>
     *              <li>{@code declineReason} — reason code from the payment processor</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code declineEmailSent} — {@code true}</li>
     *           <li>{@code declineReason} — echoed back for audit purposes</li>
     *           <li>{@code declinedAt} — ISO-8601 timestamp</li>
     *         </ul>
     */
    @JobWorker(type = "paymentDecline")
    public Map<String, Object> paymentDecline(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String customerId = (String) vars.getOrDefault("customerId", "CUST-UNKNOWN");
        String reason = (String) vars.getOrDefault("declineReason", "Insufficient funds");
        LOGGER.info("paymentDecline: customerId={}, reason={}", customerId, reason);

        Map<String, Object> result = new HashMap<>();
        result.put("declineEmailSent", true);
        result.put("declineReason", reason);
        result.put("declinedAt", Instant.now().toString());
        return result;
    }

    /**
     * Ships a tool to the customer's delivery address via a courier.
     *
     * <p><b>Business context:</b> After a rental or purchase is confirmed and
     * payment taken, ProBuild despatches the tool. This task creates the shipment
     * record, assigns a courier reference, and generates a tracking number the
     * customer can use to follow their delivery.
     *
     * <p><b>Task type:</b> {@code shipTool}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code toolId} — which tool is being shipped</li>
     *              <li>{@code orderId} — the associated order</li>
     *              <li>{@code deliveryAddress} — customer's delivery address</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code shipmentId} — internal shipment reference (prefixed "SHIP-")</li>
     *           <li>{@code trackingNumber} — courier tracking number (prefixed "TRK-")</li>
     *           <li>{@code shippedAt} — ISO-8601 timestamp</li>
     *         </ul>
     */
    @JobWorker(type = "shipTool")
    public Map<String, Object> shipTool(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String toolId = (String) vars.getOrDefault("toolId", "TOOL-UNKNOWN");
        String orderId = (String) vars.getOrDefault("orderId", "ORD-UNKNOWN");
        LOGGER.info("shipTool: toolId={}, orderId={}", toolId, orderId);

        Map<String, Object> result = new HashMap<>();
        result.put("shipmentId", "SHIP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("trackingNumber", "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("shippedAt", Instant.now().toString());
        return result;
    }

    /**
     * Verifies a customer's identity document before high-value transactions
     * or account creation.
     *
     * <p><b>Business context:</b> For rentals above a certain value, or when
     * opening a new trade account, ProBuild must confirm the customer's identity.
     * This task checks the provided document against an identity verification
     * service and records the result for compliance/audit purposes.
     *
     * <p><b>Task type:</b> {@code auditID}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code customerId} — the customer being verified</li>
     *              <li>{@code idDocumentType} — "passport", "driverLicence", or "nationalId"</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code idVerified} — {@code true} if document passed (stub: always true)</li>
     *           <li>{@code auditPassed} — {@code true} overall audit result</li>
     *           <li>{@code auditReference} — compliance reference number (prefixed "AUD-")</li>
     *         </ul>
     */
    @JobWorker(type = "auditID")
    public Map<String, Object> auditId(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String customerId = (String) vars.getOrDefault("customerId", "CUST-UNKNOWN");
        String docType = (String) vars.getOrDefault("idDocumentType", "unknown");
        LOGGER.info("auditID: customerId={}, idDocumentType={}", customerId, docType);

        Map<String, Object> result = new HashMap<>();
        result.put("idVerified", true);
        result.put("auditPassed", true);
        result.put("auditReference", "AUD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        return result;
    }
}
