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
 * Workers for the ProBuild Retail Sales Process (Process_ProBuildRetail).
 * Handles stock checks, payments, order creation and collection confirmation.
 */
@Component
public class RetailWorkers {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetailWorkers.class);

    // Inputs: productId, skuId
    // Outputs: stockAvailable (boolean), stockLevel (int)
    @JobWorker(type = "retail.stockCheck")
    public Map<String, Object> stockCheck(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String productId = (String) vars.getOrDefault("productId", vars.getOrDefault("skuId", "UNKNOWN"));
        LOGGER.info("retail.stockCheck: productId={}", productId);

        // Stub: in-store stock always available
        Map<String, Object> result = new HashMap<>();
        result.put("stockAvailable", true);
        result.put("stockLevel", 10);
        return result;
    }

    // Inputs: productId, skuId
    // Outputs: stockAvailable (boolean), stockLevel (int)
    @JobWorker(type = "retail.onlineStockCheck")
    public Map<String, Object> onlineStockCheck(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String productId = (String) vars.getOrDefault("productId", vars.getOrDefault("skuId", "UNKNOWN"));
        LOGGER.info("retail.onlineStockCheck: productId={}", productId);

        // Stub: online warehouse stock always available
        Map<String, Object> result = new HashMap<>();
        result.put("stockAvailable", true);
        result.put("stockLevel", 5);
        return result;
    }

    // Inputs: orderTotal, cardToken
    // Outputs: paymentStatus ("PAID"), transactionId, paymentProcessedAt
    // Error: throws BPMN error code "PaymentDeclined" if card is declined
    @JobWorker(type = "retail.processCardPayment")
    public Map<String, Object> processCardPayment(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        Number orderTotal = (Number) vars.getOrDefault("orderTotal", 0);
        LOGGER.info("retail.processCardPayment: orderTotal={}", orderTotal);

        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, Object> result = new HashMap<>();
        result.put("paymentStatus", "PAID");
        result.put("transactionId", transactionId);
        result.put("paymentProcessedAt", Instant.now().toString());
        return result;
    }

    // Inputs: orderTotal, paymentToken
    // Outputs: paymentStatus ("PAID"), transactionId, paymentProcessedAt
    // Error: throws BPMN error code "PaymentDeclined" if authorisation fails
    @JobWorker(type = "retail.processOnlinePayment")
    public Map<String, Object> processOnlinePayment(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        Number orderTotal = (Number) vars.getOrDefault("orderTotal", 0);
        LOGGER.info("retail.processOnlinePayment: orderTotal={}", orderTotal);

        String transactionId = "OTXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, Object> result = new HashMap<>();
        result.put("paymentStatus", "PAID");
        result.put("transactionId", transactionId);
        result.put("paymentProcessedAt", Instant.now().toString());
        return result;
    }

    // Inputs: orderTotal (click-and-collect payment)
    // Outputs: paymentStatus ("PAID"), transactionId, paymentProcessedAt
    // Error: throws BPMN error code "PaymentDeclined" if card is declined
    @JobWorker(type = "retail.processCCPayment")
    public Map<String, Object> processCCPayment(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        Number orderTotal = (Number) vars.getOrDefault("orderTotal", 0);
        LOGGER.info("retail.processCCPayment (click & collect): orderTotal={}", orderTotal);

        String transactionId = "CCTXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, Object> result = new HashMap<>();
        result.put("paymentStatus", "PAID");
        result.put("transactionId", transactionId);
        result.put("paymentProcessedAt", Instant.now().toString());
        return result;
    }

    // Inputs: customerId, orderTotal, fulfilmentMethod ("delivery" | "collect"), transactionId
    // Outputs: orderId, orderStatus ("CONFIRMED"), orderCreatedAt
    @JobWorker(type = "retail.createOrder")
    public Map<String, Object> createOrder(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String customerId = (String) vars.getOrDefault("customerId", "CUST-UNKNOWN");
        String fulfilmentMethod = (String) vars.getOrDefault("fulfilmentMethod", "delivery");
        Number orderTotal = (Number) vars.getOrDefault("orderTotal", 0);
        LOGGER.info("retail.createOrder: customerId={}, fulfilmentMethod={}, orderTotal={}",
                customerId, fulfilmentMethod, orderTotal);

        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("orderStatus", "CONFIRMED");
        result.put("orderCreatedAt", Instant.now().toString());
        return result;
    }

    // Inputs: orderId (after click-and-collect preparation)
    // Outputs: selectionConfirmed (boolean), confirmedAt
    @JobWorker(type = "retail.confirmSelection")
    public Map<String, Object> confirmSelection(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String orderId = (String) vars.getOrDefault("orderId", "UNKNOWN");
        LOGGER.info("retail.confirmSelection: orderId={}", orderId);

        Map<String, Object> result = new HashMap<>();
        result.put("selectionConfirmed", true);
        result.put("confirmedAt", Instant.now().toString());
        return result;
    }
}
