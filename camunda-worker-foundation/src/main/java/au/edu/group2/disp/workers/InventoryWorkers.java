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
 * Workers for IMS (Inventory Management System) and delivery tasks
 * in the ProBuild, Warehouse, and Supplier processes.
 */
@Component
public class InventoryWorkers {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryWorkers.class);

    // Inputs: productId, changeType ("add" | "deduct" | "adjust"), quantity, reason
    // Outputs: imsUpdated (boolean), currentStock (int), imsUpdateReference
    @JobWorker(type = "IMS")
    public Map<String, Object> imsUpdate(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String productId = (String) vars.getOrDefault("productId", "PROD-UNKNOWN");
        String changeType = (String) vars.getOrDefault("changeType", "adjust");
        Number quantity = (Number) vars.getOrDefault("quantity", 0);
        LOGGER.info("IMS: productId={}, changeType={}, quantity={}", productId, changeType, quantity);

        // Stub: assume 50 units after update. IMS is also exported as a variable named "IMS"
        // because the downstream gateway condition uses =IMS >= 10 (variable name matches job type).
        Map<String, Object> result = new HashMap<>();
        result.put("imsUpdated", true);
        result.put("currentStock", 50);
        result.put("IMS", 50);
        result.put("imsUpdateReference", "IMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("imsUpdatedAt", Instant.now().toString());
        return result;
    }

    // Inputs: productId, stockLevel (current), threshold (reorder point)
    // Outputs: imsUpdated (boolean), imsUpdateReference
    @JobWorker(type = "routineIMSUpdate")
    public Map<String, Object> routineImsUpdate(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String productId = (String) vars.getOrDefault("productId", "PROD-UNKNOWN");
        Number stockLevel = (Number) vars.getOrDefault("stockLevel", 0);
        LOGGER.info("routineIMSUpdate: productId={}, stockLevel={}", productId, stockLevel);

        Map<String, Object> result = new HashMap<>();
        result.put("imsUpdated", true);
        result.put("imsUpdateReference", "RIMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("imsUpdatedAt", Instant.now().toString());
        return result;
    }

    // Inputs: productId, newStockLevel, reason
    // Outputs: imsUpdated (boolean), imsUpdateReference
    @JobWorker(type = "IMSUpdate")
    public Map<String, Object> imsDirectUpdate(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String productId = (String) vars.getOrDefault("productId", "PROD-UNKNOWN");
        Number newStockLevel = (Number) vars.getOrDefault("newStockLevel", 0);
        LOGGER.info("IMSUpdate: productId={}, newStockLevel={}", productId, newStockLevel);

        Map<String, Object> result = new HashMap<>();
        result.put("imsUpdated", true);
        result.put("imsUpdateReference", "IMSU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("imsUpdatedAt", Instant.now().toString());
        return result;
    }

    // Inputs: productId, deductQty (units to remove after rental/sale)
    // Outputs: imsDeducted (boolean), newStockLevel (int), imsUpdateReference
    @JobWorker(type = "deductionIMS")
    public Map<String, Object> deductionIms(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String productId = (String) vars.getOrDefault("productId", "PROD-UNKNOWN");
        Number deductQty = (Number) vars.getOrDefault("deductQty", 1);
        Number currentStock = (Number) vars.getOrDefault("currentStock", 50);
        LOGGER.info("deductionIMS: productId={}, deductQty={}", productId, deductQty);

        int newStock = Math.max(currentStock.intValue() - deductQty.intValue(), 0);
        Map<String, Object> result = new HashMap<>();
        result.put("imsDeducted", true);
        result.put("newStockLevel", newStock);
        result.put("imsUpdateReference", "DED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        return result;
    }

    // Inputs: productId, warehouseId, changeType, quantity
    // Outputs: warehouseImsUpdated (boolean), warehouseStock (int), updateReference
    @JobWorker(type = "warehouse.IMS")
    public Map<String, Object> warehouseIms(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String productId = (String) vars.getOrDefault("productId", "PROD-UNKNOWN");
        String warehouseId = (String) vars.getOrDefault("warehouseId", "WH-MAIN");
        Number quantity = (Number) vars.getOrDefault("quantity", 0);
        LOGGER.info("warehouse.IMS: productId={}, warehouseId={}, quantity={}", productId, warehouseId, quantity);

        Map<String, Object> result = new HashMap<>();
        result.put("warehouseImsUpdated", true);
        result.put("warehouseStock", 100);
        result.put("updateReference", "WIMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("updatedAt", Instant.now().toString());
        return result;
    }

    // Inputs: supplierId, orderId, expectedItems
    // Outputs: deliveryStatus ("PENDING" | "IN_TRANSIT" | "DELIVERED"), estimatedArrival
    @JobWorker(type = "fetchDelivery")
    public Map<String, Object> fetchDelivery(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String supplierId = (String) vars.getOrDefault("supplierId", "SUP-UNKNOWN");
        String orderId = (String) vars.getOrDefault("orderId", "ORD-UNKNOWN");
        LOGGER.info("fetchDelivery: supplierId={}, orderId={}", supplierId, orderId);

        Map<String, Object> result = new HashMap<>();
        result.put("deliveryStatus", "IN_TRANSIT");
        result.put("estimatedArrival", LocalDateHelper.plusDays(3));
        return result;
    }

    // Inputs: orderId, deliveryAddress, courierId
    // Outputs: deliveryScheduled (boolean), courierReference
    @JobWorker(type = "Delivery")
    public Map<String, Object> delivery(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String orderId = (String) vars.getOrDefault("orderId", "ORD-UNKNOWN");
        LOGGER.info("Delivery: orderId={}", orderId);

        Map<String, Object> result = new HashMap<>();
        result.put("deliveryScheduled", true);
        result.put("courierReference", "DEL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("scheduledAt", Instant.now().toString());
        return result;
    }

    // Inputs: orderId, deliveryId, receivedBy
    // Outputs: goodsDelivered (boolean), deliverySignature, deliveredAt
    @JobWorker(type = "goodsDelivery")
    public Map<String, Object> goodsDelivery(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String orderId = (String) vars.getOrDefault("orderId", "ORD-UNKNOWN");
        LOGGER.info("goodsDelivery: orderId={}", orderId);

        Map<String, Object> result = new HashMap<>();
        result.put("goodsDelivered", true);
        result.put("deliverySignature", "SIGN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("deliveredAt", Instant.now().toString());
        return result;
    }

    // Inputs: toolId, serviceReport (string from FixPro), serviceReferenceNumber
    // Outputs: toolServiced (boolean), serviceReference, servicedAt
    @JobWorker(type = "servicedTools")
    public Map<String, Object> servicedTools(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String toolId = (String) vars.getOrDefault("toolId", "TOOL-UNKNOWN");
        String serviceRef = (String) vars.getOrDefault("serviceReferenceNumber", "FXP-UNKNOWN");
        LOGGER.info("servicedTools: toolId={}, serviceRef={}", toolId, serviceRef);

        Map<String, Object> result = new HashMap<>();
        result.put("toolServiced", true);
        result.put("serviceReference", serviceRef);
        result.put("servicedAt", Instant.now().toString());
        return result;
    }

    // Inputs: supplierId, orderId, damageLevel, damageDescription
    // Outputs: notificationSent (boolean), notificationReference, notifiedAt
    @JobWorker(type = "failedInspectionNotification")
    public Map<String, Object> failedInspectionNotification(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String supplierId = (String) vars.getOrDefault("supplierId", "SUP-UNKNOWN");
        String orderId = (String) vars.getOrDefault("orderId", "ORD-UNKNOWN");
        String damageLevel = (String) vars.getOrDefault("damageLevel", "minor");
        LOGGER.info("failedInspectionNotification: supplierId={}, orderId={}, damageLevel={}",
                supplierId, orderId, damageLevel);

        Map<String, Object> result = new HashMap<>();
        result.put("notificationSent", true);
        result.put("notificationReference", "FAIL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("notifiedAt", Instant.now().toString());
        return result;
    }

    // Inputs: productId, stockDelta (positive = restock, negative = sale/use), reason
    // Outputs: imsUpdated (boolean), newStockLevel (int), updateReference
    @JobWorker(type = "updateStock")
    public Map<String, Object> updateStock(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String productId = (String) vars.getOrDefault("productId", "PROD-UNKNOWN");
        Number stockDelta = (Number) vars.getOrDefault("stockDelta", 0);
        Number currentStock = (Number) vars.getOrDefault("currentStock", 50);
        LOGGER.info("updateStock: productId={}, stockDelta={}", productId, stockDelta);

        int newStockLevel = Math.max(currentStock.intValue() + stockDelta.intValue(), 0);
        Map<String, Object> result = new HashMap<>();
        result.put("imsUpdated", true);
        result.put("newStockLevel", newStockLevel);
        result.put("updateReference", "UPD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("updatedAt", Instant.now().toString());
        return result;
    }

    // Inputs: productId, customerId, requestedQty
    // Outputs: available (boolean), availableQty (int), expectedRestockDate
    @JobWorker(type = "customerIMSAvail")
    public Map<String, Object> customerImsAvail(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String productId = (String) vars.getOrDefault("productId", "PROD-UNKNOWN");
        Number requestedQty = (Number) vars.getOrDefault("requestedQty", 1);
        LOGGER.info("customerIMSAvail: productId={}, requestedQty={}", productId, requestedQty);

        // Stub: product always available with 10 units in stock
        Map<String, Object> result = new HashMap<>();
        result.put("available", true);
        result.put("availableQty", 10);
        result.put("onlineavailability", 10);
        result.put("expectedRestockDate", LocalDateHelper.plusDays(7));
        return result;
    }

    // Inputs: orderId, deliveryAddress, productId, quantity
    // Outputs: goodsShipped (boolean), shipmentReference, estimatedDelivery
    @JobWorker(type = "shipGoods")
    public Map<String, Object> shipGoods(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String orderId = (String) vars.getOrDefault("orderId", "ORD-UNKNOWN");
        String deliveryAddress = (String) vars.getOrDefault("deliveryAddress", "Unknown Address");
        LOGGER.info("shipGoods: orderId={}, deliveryAddress={}", orderId, deliveryAddress);

        Map<String, Object> result = new HashMap<>();
        result.put("goodsShipped", true);
        result.put("shipmentReference", "SHIP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("estimatedDelivery", LocalDateHelper.plusDays(3));
        result.put("shippedAt", Instant.now().toString());
        return result;
    }

    // Small helper to avoid a java.time dependency in method bodies
    private static class LocalDateHelper {
        static String plusDays(int days) {
            return java.time.LocalDate.now().plusDays(days).toString();
        }
    }
}
