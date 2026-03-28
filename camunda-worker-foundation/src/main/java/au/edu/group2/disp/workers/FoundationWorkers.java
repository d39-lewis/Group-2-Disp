package au.edu.group2.disp.workers;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FoundationWorkers {

    private static final Logger LOGGER = LoggerFactory.getLogger(FoundationWorkers.class);

    @JobWorker(type = "processOrder")
    public Map<String, Object> processOrder(ActivatedJob job) {
        Map<String, Object> vars = safeVars(job);
        LOGGER.info("processOrder activated. jobKey={}, processInstanceKey={}", job.getKey(), job.getProcessInstanceKey());

        String orderId = String.valueOf(vars.getOrDefault("orderId", "ORDER-" + Instant.now().toEpochMilli()));

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("orderCaptured", true);
        result.put("orderCapturedAt", Instant.now().toString());
        return result;
    }

    @JobWorker(type = "processOrderInPerson")
    public Map<String, Object> processOrderInPerson(ActivatedJob job) {
        LOGGER.info("processOrderInPerson activated. jobKey={}, processInstanceKey={}", job.getKey(), job.getProcessInstanceKey());

        Map<String, Object> result = new HashMap<>();
        result.put("inPersonOrderCaptured", true);
        result.put("orderCapturedAt", Instant.now().toString());
        return result;
    }

    @JobWorker(type = "financeRequest")
    public Map<String, Object> financeRequest(ActivatedJob job) {
        Map<String, Object> vars = safeVars(job);
        LOGGER.info("financeRequest activated. jobKey={}, processInstanceKey={}", job.getKey(), job.getProcessInstanceKey());

        Number amount = (Number) vars.getOrDefault("amount", 0);
        boolean approved = amount.doubleValue() <= 10000;

        Map<String, Object> result = new HashMap<>();
        result.put("financeApproved", approved);
        result.put("financeReviewedAt", Instant.now().toString());
        result.put("financeDecisionReason", approved ? "Auto-approved within foundation threshold" : "Needs manual review");
        return result;
    }

    @JobWorker(type = "payBill")
    public Map<String, Object> payBill(ActivatedJob job) {
        LOGGER.info("payBill activated. jobKey={}, processInstanceKey={}", job.getKey(), job.getProcessInstanceKey());

        Map<String, Object> result = new HashMap<>();
        result.put("paymentStatus", "PAID");
        result.put("paymentProcessedAt", Instant.now().toString());
        return result;
    }

    private Map<String, Object> safeVars(ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        return vars == null ? Map.of() : vars;
    }
}
