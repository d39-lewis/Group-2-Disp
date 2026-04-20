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
 * Workers for the main ProBuild process (Pro).
 * Covers payment calculation, card processing, receipts, membership,
 * credentials, trade accounts, queue management, and audit.
 */
@Component
public class ProBuildWorkers {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProBuildWorkers.class);

    // Inputs: items (list), rentalDays, unitPrice, membershipDiscount
    // Outputs: orderTotal (double), itemCount (int)
    @JobWorker(type = "calculate-order-amount")
    public Map<String, Object> calculateOrderAmount(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        Number unitPrice = (Number) vars.getOrDefault("unitPrice", 0);
        Number rentalDays = (Number) vars.getOrDefault("rentalDays", 1);
        Number quantity = (Number) vars.getOrDefault("quantity", 1);
        LOGGER.info("calculate-order-amount: unitPrice={}, rentalDays={}, quantity={}", unitPrice, rentalDays, quantity);

        double orderTotal = unitPrice.doubleValue() * rentalDays.doubleValue() * quantity.doubleValue();
        Map<String, Object> result = new HashMap<>();
        result.put("orderTotal", orderTotal);
        result.put("itemCount", quantity.intValue());
        return result;
    }

    // Inputs: orderTotal, cardToken
    // Outputs: paymentSuccessful ("yes" | "no"), transactionId, paymentProcessedAt
    // Gateway downstream reads: =paymentSuccessful = "yes" / "no"
    @JobWorker(type = "receive-card-payment")
    public Map<String, Object> receiveCardPayment(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        Number orderTotal = (Number) vars.getOrDefault("orderTotal", 0);
        LOGGER.info("receive-card-payment: orderTotal={}", orderTotal);

        String transactionId = "CARD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, Object> result = new HashMap<>();
        result.put("paymentSuccessful", "yes");
        result.put("transactionId", transactionId);
        result.put("paymentProcessedAt", Instant.now().toString());
        return result;
    }

    // Inputs: orderId, orderTotal, transactionId, customerEmail
    // Outputs: receiptPrinted (boolean), receiptReference
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

    // Inputs: orderTotal, paymentMethod ("cash" | "card"), orderId
    // Outputs: posTransactionId, transactionComplete (boolean), posProcessedAt
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

    // Inputs: customerId, memberEmail, membershipId
    // Outputs: credentialsValid (boolean), membershipActive (boolean)
    // Used for both "Check if credentials are valid" and "display failed inputs" tasks
    @JobWorker(type = "credentialCheck")
    public Map<String, Object> credentialCheck(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String customerId = (String) vars.getOrDefault("customerId", "CUST-UNKNOWN");
        LOGGER.info("credentialCheck: customerId={}", customerId);

        // Stub: credentials always valid
        Map<String, Object> result = new HashMap<>();
        result.put("credentialsValid", true);
        result.put("membershipActive", true);
        return result;
    }

    // Inputs: customerId, membershipType ("standard" | "trade" | "premium"), membershipId
    // Outputs: membershipUpdated (boolean), membershipUpdatedAt
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

    // Inputs: customerId, membershipId, customerEmail
    // Outputs: membershipConfirmed (boolean), confirmationSentAt
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

    // Inputs: abn (Australian Business Number) or businessId
    // Outputs: tradeAccountActive (boolean), discountRate (double), tradeCategory
    @JobWorker(type = "tradeDatabase")
    public Map<String, Object> tradeDatabase(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String businessId = (String) vars.getOrDefault("businessId", vars.getOrDefault("abn", "UNKNOWN"));
        LOGGER.info("tradeDatabase: businessId={}", businessId);

        // Stub: trade account always active with 10% discount
        Map<String, Object> result = new HashMap<>();
        result.put("tradeAccountActive", true);
        result.put("discountRate", 0.10);
        result.put("tradeCategory", "standard");
        return result;
    }

    // Inputs: orderTotal, membershipType ("standard" | "trade" | "premium")
    // Outputs: discountedTotal (double), discountApplied (boolean), discountAmount (double)
    @JobWorker(type = "membershipDiscount")
    public Map<String, Object> membershipDiscount(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        Number orderTotal = (Number) vars.getOrDefault("orderTotal", 0);
        String membershipType = (String) vars.getOrDefault("membershipType", "standard");
        LOGGER.info("membershipDiscount: orderTotal={}, membershipType={}", orderTotal, membershipType);

        double rate = switch (membershipType) {
            case "premium" -> 0.15;
            case "trade" -> 0.10;
            default -> 0.05;
        };
        double discountAmount = orderTotal.doubleValue() * rate;
        double discountedTotal = orderTotal.doubleValue() - discountAmount;

        Map<String, Object> result = new HashMap<>();
        result.put("discountedTotal", discountedTotal);
        result.put("discountApplied", true);
        result.put("discountAmount", discountAmount);
        result.put("orderTotal", discountedTotal);
        return result;
    }

    // Inputs: customerId, isMember (boolean)
    // Outputs: queuePosition (int), estimatedWaitMinutes (int)
    // Members get priority (lower queue position)
    @JobWorker(type = "Queue")
    public Map<String, Object> queue(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String customerId = (String) vars.getOrDefault("customerId", "CUST-UNKNOWN");
        boolean isMember = Boolean.parseBoolean(String.valueOf(vars.getOrDefault("isMember", false)));
        LOGGER.info("Queue: customerId={}, isMember={}", customerId, isMember);

        int position = isMember ? 1 : 5;
        Map<String, Object> result = new HashMap<>();
        result.put("queuePosition", position);
        result.put("estimatedWaitMinutes", position * 5);
        return result;
    }

    // Inputs: customerEmail, queuePosition, estimatedWaitMinutes
    // Outputs: emailSent (boolean), emailSentAt
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

    // Inputs: customerId, orderTotal, declineReason
    // Outputs: declineEmailSent (boolean), declineReason
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

    // Inputs: toolId, orderId, deliveryAddress, customerId
    // Outputs: shipmentId, trackingNumber, shippedAt
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

    // Inputs: customerId, idDocumentType ("passport" | "driverLicence" | "nationalId")
    // Outputs: idVerified (boolean), auditPassed (boolean), auditReference
    @JobWorker(type = "auditID")
    public Map<String, Object> auditId(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String customerId = (String) vars.getOrDefault("customerId", "CUST-UNKNOWN");
        String docType = (String) vars.getOrDefault("idDocumentType", "unknown");
        LOGGER.info("auditID: customerId={}, idDocumentType={}", customerId, docType);

        // Stub: ID always passes
        Map<String, Object> result = new HashMap<>();
        result.put("idVerified", true);
        result.put("auditPassed", true);
        result.put("auditReference", "AUD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        return result;
    }
}
