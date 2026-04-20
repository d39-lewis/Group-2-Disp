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
 * Workers for the <b>ProBuild Retail Sales Process</b> ({@code Process_ProBuildRetail}).
 *
 * <p>This process handles all point-of-sale and online retail transactions —
 * from the moment an order arrives, through stock checking and payment, to
 * fulfilment (delivery or click-and-collect).
 *
 * <p><b>Happy-path flow (in-store):</b>
 * <ol>
 *   <li>Order received → gateway splits on {@code orderChannel}</li>
 *   <li>{@code retail.stockCheck} — confirms item is on the shelf</li>
 *   <li>Staff scan item at POS (user task — human step)</li>
 *   <li>Gateway splits on {@code paymentMethod}</li>
 *   <li>{@code retail.processCardPayment} (card) or straight to receipt (cash)</li>
 *   <li>Receipt printed (user task)</li>
 * </ol>
 *
 * <p><b>Happy-path flow (online):</b>
 * <ol>
 *   <li>{@code retail.onlineStockCheck} — confirms warehouse availability</li>
 *   <li>Gateway splits on fulfilment: delivery or click-and-collect</li>
 *   <li>{@code retail.processOnlinePayment} (delivery) or {@code retail.processCCPayment} (C&C)</li>
 *   <li>{@code retail.createOrder} — writes the confirmed order to the order system</li>
 *   <li>Pick, pack, dispatch (user tasks) — or {@code retail.confirmSelection} for C&C</li>
 * </ol>
 *
 * <p><b>Error paths:</b> The BPMN attaches a boundary error event
 * ({@code Error_PaymentDeclined}) to each payment task. In production you would
 * throw that error from the worker when the payment gateway declines the card.
 * Currently all payment workers complete successfully (happy-path stub).
 */
@Component
public class RetailWorkers {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetailWorkers.class);

    /**
     * Checks whether a product is physically available on the shop floor.
     *
     * <p><b>Business context:</b> Before a staff member scans an item, the system
     * confirms it is actually in stock. If {@code stockAvailable} comes back
     * {@code false}, the process ends with an "Out of stock (in-store)" event and
     * the customer is informed.
     *
     * <p><b>Task type:</b> {@code retail.stockCheck}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code productId} or {@code skuId} — the item being checked</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code stockAvailable} — {@code true} if item can be sold</li>
     *           <li>{@code stockLevel} — current quantity on shelf (stub: always 10)</li>
     *         </ul>
     */
    @JobWorker(type = "retail.stockCheck")
    public Map<String, Object> stockCheck(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String productId = (String) vars.getOrDefault("productId", vars.getOrDefault("skuId", "UNKNOWN"));
        LOGGER.info("retail.stockCheck: productId={}", productId);

        Map<String, Object> result = new HashMap<>();
        result.put("stockAvailable", true);
        result.put("stockLevel", 10);
        return result;
    }

    /**
     * Checks whether a product is available in the online warehouse for despatch.
     *
     * <p><b>Business context:</b> Identical logic to {@code retail.stockCheck} but
     * queries the online/warehouse inventory rather than the shop floor. A separate
     * task type exists because the two stock pools are managed independently —
     * a product sold out in-store may still be available for online order and
     * vice versa.
     *
     * <p><b>Task type:</b> {@code retail.onlineStockCheck}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code productId} or {@code skuId}</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code stockAvailable} — {@code true} if item can be despatched</li>
     *           <li>{@code stockLevel} — current warehouse quantity (stub: always 5)</li>
     *         </ul>
     */
    @JobWorker(type = "retail.onlineStockCheck")
    public Map<String, Object> onlineStockCheck(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String productId = (String) vars.getOrDefault("productId", vars.getOrDefault("skuId", "UNKNOWN"));
        LOGGER.info("retail.onlineStockCheck: productId={}", productId);

        Map<String, Object> result = new HashMap<>();
        result.put("stockAvailable", true);
        result.put("stockLevel", 5);
        return result;
    }

    /**
     * Processes a card payment at the physical point-of-sale terminal.
     *
     * <p><b>Business context:</b> The customer has chosen to pay by card at the
     * counter. This worker submits the payment to the card processor and records
     * the result. On success, the process moves to printing the receipt.
     * On failure, the BPMN boundary error event fires and the customer may retry
     * or cancel.
     *
     * <p><b>Task type:</b> {@code retail.processCardPayment}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code orderTotal} — amount to charge in GBP</li>
     *              <li>{@code cardToken} — tokenised card reference from the POS terminal</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code paymentStatus} — "PAID"</li>
     *           <li>{@code transactionId} — unique payment reference (prefixed "TXN-")</li>
     *           <li>{@code paymentProcessedAt} — ISO-8601 timestamp</li>
     *         </ul>
     */
    @JobWorker(type = "retail.processCardPayment")
    public Map<String, Object> processCardPayment(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        Number orderTotal = (Number) vars.getOrDefault("orderTotal", 0);
        LOGGER.info("retail.processCardPayment: orderTotal={}", orderTotal);

        Map<String, Object> result = new HashMap<>();
        result.put("paymentStatus", "PAID");
        result.put("transactionId", "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("paymentProcessedAt", Instant.now().toString());
        return result;
    }

    /**
     * Processes a card payment for a home-delivery online order.
     *
     * <p><b>Business context:</b> The customer has placed an order online and
     * chosen delivery. Payment is taken at this point before the order is
     * committed to the fulfilment system. On failure, the boundary error event
     * fires and the order is cancelled.
     *
     * <p><b>Task type:</b> {@code retail.processOnlinePayment}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code orderTotal} — amount to charge in GBP</li>
     *              <li>{@code paymentToken} — tokenised card from the online checkout</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code paymentStatus} — "PAID"</li>
     *           <li>{@code transactionId} — unique reference (prefixed "OTXN-")</li>
     *           <li>{@code paymentProcessedAt} — ISO-8601 timestamp</li>
     *         </ul>
     */
    @JobWorker(type = "retail.processOnlinePayment")
    public Map<String, Object> processOnlinePayment(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        Number orderTotal = (Number) vars.getOrDefault("orderTotal", 0);
        LOGGER.info("retail.processOnlinePayment: orderTotal={}", orderTotal);

        Map<String, Object> result = new HashMap<>();
        result.put("paymentStatus", "PAID");
        result.put("transactionId", "OTXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("paymentProcessedAt", Instant.now().toString());
        return result;
    }

    /**
     * Processes payment for a click-and-collect online order.
     *
     * <p><b>Business context:</b> The customer ordered online but will collect
     * from the store rather than have it delivered. Payment is still taken online
     * at this step. The "CC" prefix in the transaction ID distinguishes these
     * payments in the finance ledger from delivery orders.
     *
     * <p><b>Task type:</b> {@code retail.processCCPayment}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code orderTotal} — amount to charge in GBP</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code paymentStatus} — "PAID"</li>
     *           <li>{@code transactionId} — unique reference (prefixed "CCTXN-")</li>
     *           <li>{@code paymentProcessedAt} — ISO-8601 timestamp</li>
     *         </ul>
     */
    @JobWorker(type = "retail.processCCPayment")
    public Map<String, Object> processCCPayment(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        Number orderTotal = (Number) vars.getOrDefault("orderTotal", 0);
        LOGGER.info("retail.processCCPayment (click & collect): orderTotal={}", orderTotal);

        Map<String, Object> result = new HashMap<>();
        result.put("paymentStatus", "PAID");
        result.put("transactionId", "CCTXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("paymentProcessedAt", Instant.now().toString());
        return result;
    }

    /**
     * Creates a confirmed order record in the order management system.
     *
     * <p><b>Business context:</b> Called after online payment succeeds. At this
     * point money has been taken, so we commit the order. The generated
     * {@code orderId} is used by all subsequent fulfilment tasks (pick, pack,
     * despatch or click-and-collect preparation).
     *
     * <p><b>Task type:</b> {@code retail.createOrder}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code customerId} — who placed the order</li>
     *              <li>{@code orderTotal} — confirmed amount charged</li>
     *              <li>{@code fulfilmentMethod} — "delivery" or "collect"</li>
     *              <li>{@code transactionId} — payment reference to attach to the order</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code orderId} — unique order reference (prefixed "ORD-")</li>
     *           <li>{@code orderStatus} — "CONFIRMED"</li>
     *           <li>{@code orderCreatedAt} — ISO-8601 timestamp</li>
     *         </ul>
     */
    @JobWorker(type = "retail.createOrder")
    public Map<String, Object> createOrder(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String customerId = (String) vars.getOrDefault("customerId", "CUST-UNKNOWN");
        String fulfilmentMethod = (String) vars.getOrDefault("fulfilmentMethod", "delivery");
        Number orderTotal = (Number) vars.getOrDefault("orderTotal", 0);
        LOGGER.info("retail.createOrder: customerId={}, fulfilmentMethod={}, orderTotal={}",
                customerId, fulfilmentMethod, orderTotal);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("orderStatus", "CONFIRMED");
        result.put("orderCreatedAt", Instant.now().toString());
        return result;
    }

    /**
     * Confirms that a click-and-collect order has been prepared and is ready
     * for the customer to pick up.
     *
     * <p><b>Business context:</b> After staff have prepared the goods for
     * collection (a human user task), this service task updates the order
     * system to "ready for collection" and triggers any customer notification
     * (e.g. a "your order is ready" email in a real integration).
     *
     * <p><b>Task type:</b> {@code retail.confirmSelection}
     *
     * @param job the activated job. Expected variables:
     *            <ul>
     *              <li>{@code orderId} — the order being confirmed</li>
     *            </ul>
     * @return map of output variables:
     *         <ul>
     *           <li>{@code selectionConfirmed} — {@code true}</li>
     *           <li>{@code confirmedAt} — ISO-8601 timestamp</li>
     *         </ul>
     */
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
