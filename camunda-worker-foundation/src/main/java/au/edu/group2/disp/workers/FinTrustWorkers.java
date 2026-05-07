package au.edu.group2.disp.workers;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Workers for the FinTrust finance process (Process_1kwzimz).
 * Handles installment plan calculation, monthly email notifications,
 * finance confirmation, and installment completion.
 * The 6Installment / 12Installment types use numeric-leading names exactly as
 * defined in the BPMN zeebe:taskDefinition elements.
 */
@Component
public class FinTrustWorkers {

    private static final Logger LOGGER = LoggerFactory.getLogger(FinTrustWorkers.class);

    // Applied 5% annual interest rate for installment products
    private static final double INTEREST_RATE = 0.05;

    // Inputs: orderTotal
    // Outputs: monthlyAmount (double), totalWithInterest (double), installments (int)
    @JobWorker(type = "6Installment")
    public Map<String, Object> sixInstallment(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        Number orderTotal = (Number) vars.getOrDefault("orderTotal", 0);
        LOGGER.info("6Installment: orderTotal={}", orderTotal);

        double totalWithInterest = orderTotal.doubleValue() * (1 + INTEREST_RATE);
        double monthly = Math.round((totalWithInterest / 6) * 100.0) / 100.0;

        Map<String, Object> result = new HashMap<>();
        result.put("monthlyAmount", monthly);
        result.put("totalWithInterest", Math.round(totalWithInterest * 100.0) / 100.0);
        result.put("financeInstallments", 6);
        return result;
    }

    // Inputs: orderTotal
    // Outputs: monthlyAmount (double), totalWithInterest (double), installments (int)
    @JobWorker(type = "12Installment")
    public Map<String, Object> twelveInstallment(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        Number orderTotal = (Number) vars.getOrDefault("orderTotal", 0);
        LOGGER.info("12Installment: orderTotal={}", orderTotal);

        double totalWithInterest = orderTotal.doubleValue() * (1 + INTEREST_RATE);
        double monthly = Math.round((totalWithInterest / 12) * 100.0) / 100.0;

        Map<String, Object> result = new HashMap<>();
        result.put("monthlyAmount", monthly);
        result.put("totalWithInterest", Math.round(totalWithInterest * 100.0) / 100.0);
        result.put("financeInstallments", 12);
        return result;
    }

    // Inputs: customerEmail, monthlyAmount, installmentNumber, totalInstallments, financeRequestId
    // Outputs: emailSent (boolean), emailSentAt
    @JobWorker(type = "financeEmail")
    public Map<String, Object> financeEmail(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String customerEmail = (String) vars.getOrDefault("customerEmail", "unknown@example.com");
        Number monthlyAmount = (Number) vars.getOrDefault("monthlyAmount", 0);
        Number installmentNumber = (Number) vars.getOrDefault("installmentNumber", 1);
        // financeInstallments may arrive as String "6"/"12" from Website.form or as Integer from 6Installment/12Installment workers
        Object rawInstallments = vars.getOrDefault("financeInstallments", 12);
        int totalInstallments = rawInstallments instanceof Number
                ? ((Number) rawInstallments).intValue()
                : Integer.parseInt(rawInstallments.toString());
        LOGGER.info("financeEmail: email={}, installment={}/{}, amount={}",
                customerEmail, installmentNumber, totalInstallments, monthlyAmount);

        Map<String, Object> result = new HashMap<>();
        result.put("emailSent", true);
        result.put("emailSentAt", Instant.now().toString());
        return result;
    }

    // Inputs: financeRequestId, customerId
    // Outputs: financeConfirmation (boolean), financeConfirmedAt
    @JobWorker(type = "financeConfirmation")
    public Map<String, Object> financeConfirmation(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String financeRequestId = (String) vars.getOrDefault("financeRequestId", "FIN-UNKNOWN");
        LOGGER.info("financeConfirmation: financeRequestId={}", financeRequestId);

        Map<String, Object> result = new HashMap<>();
        result.put("financeConfirmation", true);
        result.put("financeConfirmedAt", Instant.now().toString());
        return result;
    }

    // Inputs: financeRequestId, customerId
    // Outputs: financeCompleted (boolean), completedAt
    @JobWorker(type = "installmentCompletion")
    public Map<String, Object> installmentCompletion(ActivatedJob job) {
        Map<String, Object> vars = FoundationWorkers.safeVars(job);
        String financeRequestId = (String) vars.getOrDefault("financeRequestId", "FIN-UNKNOWN");
        LOGGER.info("installmentCompletion: financeRequestId={}", financeRequestId);

        Map<String, Object> result = new HashMap<>();
        result.put("financeCompleted", true);
        result.put("completedAt", Instant.now().toString());
        return result;
    }
}
