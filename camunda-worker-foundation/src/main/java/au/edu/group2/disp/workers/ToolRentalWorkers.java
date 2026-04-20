package au.edu.group2.disp.workers;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Workers for tool rental, damage assessment, and maintenance tasks in the ProBuild process.
 */
@Component
public class ToolRentalWorkers {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolRentalWorkers.class);

    private static final double LATE_FEE_PER_DAY = 15.00;

    // Inputs: toolId, warehouseLocation
    // Outputs: toolInStock (boolean), toolLocation (string)
    @JobWorker(type = "toolAvailable")
    public Map<String, Object> toolAvailable(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String toolId = (String) vars.getOrDefault("toolId", "TOOL-UNKNOWN");
        LOGGER.info("toolAvailable: toolId={}", toolId);

        // Stub: tool always in stock at main warehouse
        Map<String, Object> result = new HashMap<>();
        result.put("toolInStock", true);
        result.put("toolLocation", "Main Warehouse - Aisle 3");
        return result;
    }

    // Inputs: toolId, rentalId, orderId
    // Outputs: toolRetrieved (boolean), toolCondition ("good" | "damaged" | "missing")
    @JobWorker(type = "storeRetrieveTool")
    public Map<String, Object> storeRetrieveTool(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String toolId = (String) vars.getOrDefault("toolId", "TOOL-UNKNOWN");
        String rentalId = (String) vars.getOrDefault("rentalId", "RENT-UNKNOWN");
        LOGGER.info("storeRetrieveTool: toolId={}, rentalId={}", toolId, rentalId);

        Map<String, Object> result = new HashMap<>();
        result.put("toolRetrieved", true);
        result.put("toolCondition", "good");
        return result;
    }

    // Inputs: rentalId, rentalEndDate (ISO-8601 date string), rentalDailyRate
    // Outputs: rentalExpired (boolean), daysOverdue (int), lateFee (double)
    @JobWorker(type = "rentalExpiry")
    public Map<String, Object> rentalExpiry(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String rentalId = (String) vars.getOrDefault("rentalId", "RENT-UNKNOWN");
        String rentalEndDateStr = (String) vars.getOrDefault("rentalEndDate", LocalDate.now().toString());
        LOGGER.info("rentalExpiry: rentalId={}, rentalEndDate={}", rentalId, rentalEndDateStr);

        LocalDate endDate = LocalDate.parse(rentalEndDateStr);
        LocalDate today = LocalDate.now();
        long daysOverdue = ChronoUnit.DAYS.between(endDate, today);

        boolean expired = daysOverdue >= 0;
        double lateFee = expired ? daysOverdue * LATE_FEE_PER_DAY : 0.0;

        Map<String, Object> result = new HashMap<>();
        result.put("rentalExpired", expired);
        result.put("daysOverdue", (int) Math.max(daysOverdue, 0));
        result.put("lateFee", lateFee);
        return result;
    }

    // Inputs: rentalId, toolCondition ("good" | "damaged"), rentalEndDate, depositAmount
    // Outputs: rentalClosed (boolean), depositRefunded (double), lateFee (double), damageDeduction (double)
    @JobWorker(type = "returnRental")
    public Map<String, Object> returnRental(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String rentalId = (String) vars.getOrDefault("rentalId", "RENT-UNKNOWN");
        String toolCondition = (String) vars.getOrDefault("toolCondition", "good");
        Number depositAmount = (Number) vars.getOrDefault("depositAmount", 100.0);
        String rentalEndDateStr = (String) vars.getOrDefault("rentalEndDate", LocalDate.now().toString());
        LOGGER.info("returnRental: rentalId={}, toolCondition={}, deposit={}",
                rentalId, toolCondition, depositAmount);

        LocalDate endDate = LocalDate.parse(rentalEndDateStr);
        long daysOverdue = ChronoUnit.DAYS.between(endDate, LocalDate.now());
        double lateFee = Math.max(daysOverdue, 0) * LATE_FEE_PER_DAY;
        double damageDeduction = "damaged".equals(toolCondition) ? depositAmount.doubleValue() * 0.5 : 0.0;
        double refund = depositAmount.doubleValue() - lateFee - damageDeduction;

        Map<String, Object> result = new HashMap<>();
        result.put("rentalClosed", true);
        result.put("depositRefunded", Math.max(refund, 0.0));
        result.put("lateFee", lateFee);
        result.put("damageDeduction", damageDeduction);
        result.put("rentalClosedAt", Instant.now().toString());
        return result;
    }

    // Inputs: rentalId, damageDeduction (amount to withhold from deposit)
    // Outputs: refundAmount (double), refundProcessed (boolean), refundReference
    @JobWorker(type = "depositPartialRefund")
    public Map<String, Object> depositPartialRefund(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String rentalId = (String) vars.getOrDefault("rentalId", "RENT-UNKNOWN");
        Number depositAmount = (Number) vars.getOrDefault("depositAmount", 100.0);
        Number damageDeduction = (Number) vars.getOrDefault("damageDeduction", 0.0);
        LOGGER.info("depositPartialRefund: rentalId={}, deposit={}, deduction={}",
                rentalId, depositAmount, damageDeduction);

        double refund = depositAmount.doubleValue() - damageDeduction.doubleValue();
        Map<String, Object> result = new HashMap<>();
        result.put("refundAmount", Math.max(refund, 0.0));
        result.put("refundProcessed", true);
        result.put("refundReference", "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("refundProcessedAt", Instant.now().toString());
        return result;
    }

    // Inputs: toolId, damageLevel ("minor" | "major" | "write-off"), damageDescription
    // Outputs: repairCost (double), damageCategory (string)
    @JobWorker(type = "damageCost")
    public Map<String, Object> damageCost(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String toolId = (String) vars.getOrDefault("toolId", "TOOL-UNKNOWN");
        String damageLevel = (String) vars.getOrDefault("damageLevel", "minor");
        LOGGER.info("damageCost: toolId={}, damageLevel={}", toolId, damageLevel);

        double repairCost = switch (damageLevel) {
            case "major" -> 250.0;
            case "write-off" -> 800.0;
            default -> 50.0;
        };

        Map<String, Object> result = new HashMap<>();
        result.put("repairCost", repairCost);
        result.put("damageCategory", damageLevel);
        return result;
    }

    // Inputs: toolId, maintenanceType ("routine" | "repair" | "service"), technicianId
    // Outputs: maintenanceLogged (boolean), logId (string)
    @JobWorker(type = "maintenaceLog")
    public Map<String, Object> maintenanceLog(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String toolId = (String) vars.getOrDefault("toolId", "TOOL-UNKNOWN");
        String maintenanceType = (String) vars.getOrDefault("maintenanceType", "routine");
        LOGGER.info("maintenaceLog: toolId={}, maintenanceType={}", toolId, maintenanceType);

        Map<String, Object> result = new HashMap<>();
        result.put("maintenanceLogged", true);
        result.put("logId", "MLOG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("loggedAt", Instant.now().toString());
        return result;
    }

    // Inputs: orderTotal, installments (int — e.g. 6 or 12), interestRate (default 0.05)
    // Outputs: installmentAmount (double), totalWithInterest (double)
    @JobWorker(type = "calculate-installment-amount")
    public Map<String, Object> calculateInstallmentAmount(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        Number orderTotal = (Number) vars.getOrDefault("orderTotal", 0);
        Number installments = (Number) vars.getOrDefault("installments", 12);
        Number interestRate = (Number) vars.getOrDefault("interestRate", 0.05);
        LOGGER.info("calculate-installment-amount: orderTotal={}, installments={}", orderTotal, installments);

        double totalWithInterest = orderTotal.doubleValue() * (1 + interestRate.doubleValue());
        double installmentAmount = totalWithInterest / installments.intValue();

        Map<String, Object> result = new HashMap<>();
        result.put("installmentAmount", Math.round(installmentAmount * 100.0) / 100.0);
        result.put("totalWithInterest", Math.round(totalWithInterest * 100.0) / 100.0);
        return result;
    }

    // Inputs: financeId, totalInstallments, paidInstallments
    // Outputs: allInstallmentsPaid (boolean), completedAt
    @JobWorker(type = "installmentComplete")
    public Map<String, Object> installmentComplete(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String financeId = (String) vars.getOrDefault("financeId", "FIN-UNKNOWN");
        Number total = (Number) vars.getOrDefault("totalInstallments", 12);
        Number paid = (Number) vars.getOrDefault("paidInstallments", 12);
        LOGGER.info("installmentComplete: financeId={}, paid={}/{}", financeId, paid, total);

        boolean allPaid = paid.intValue() >= total.intValue();
        Map<String, Object> result = new HashMap<>();
        result.put("allInstallmentsPaid", allPaid);
        result.put("completedAt", Instant.now().toString());
        return result;
    }

    // Inputs: toolId, damageReport (string), rentalId
    // Outputs: sentToFixPro (boolean), serviceReferenceNumber (string)
    @JobWorker(type = "sendFixPro")
    public Map<String, Object> sendFixPro(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String toolId = (String) vars.getOrDefault("toolId", "TOOL-UNKNOWN");
        String damageReport = (String) vars.getOrDefault("damageReport", "No report provided");
        LOGGER.info("sendFixPro: toolId={}, damageReport={}", toolId, damageReport);

        Map<String, Object> result = new HashMap<>();
        result.put("sentToFixPro", true);
        result.put("serviceReferenceNumber", "FXP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("sentAt", Instant.now().toString());
        return result;
    }
}
