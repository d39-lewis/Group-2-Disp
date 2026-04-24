# Camunda 8 testing outline (local self-managed setup)

## 1) Preconditions

- Local Camunda 8 Docker Compose environment is running (`http://localhost:8080`).
- BPMN in cluster is confirmed identical to [../daniel_local.bpmn](../daniel_local.bpmn) (or latest local variant).
- Worker app uses local self-managed config (`camunda.client.mode=self-managed`).
- Target process IDs are executable in deployed model.

## 2) Worker registration check

- Start worker app.
- Confirm logs show subscription activation for:
  - `processOrder`
  - `processOrderInPerson`
  - `financeRequest`
  - `payBill`

## 3) Happy path scenarios

1. Online order
   - Start instance with baseline variables.
   - Expect `processOrder` then downstream payment path.
2. In-person order
   - Start instance with in-store order variables.
   - Expect `processOrderInPerson` and billing path.
3. Finance request
   - Start with amount within threshold.
   - Expect `financeApproved=true`.

## 4) Failure path scenarios

- Missing required order fields.
- Finance amount above threshold (manual review branch expected).
- Payment processing simulation failure (once real logic is added).

## 5) Evidence checklist

- Operate screenshot of each completed process instance.
- Operate incident screenshot for one negative test.
- Worker logs containing job key and process instance key.
- Mapping table update in [service-task-traceability.md](service-task-traceability.md).

## 6) Regression gate for later sprints

- No new BPMN job type is allowed without a worker stub and test case.
- Any variable contract change must update this document and worker method signature/logic.
