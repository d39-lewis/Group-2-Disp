# Service Task Traceability (foundation)

Source BPMN: [../daniel_local.bpmn](../daniel_local.bpmn)
Source task notes: [../service+task+descriptions.doc](../service+task+descriptions.doc)

## Initial worker scope (phase 1)

| Job type | Present in BPMN | Present in service doc | Worker status | Notes |
|---|---|---|---|---|
| `processOrder` | Yes | Yes | Stubbed | Foundational mapping done |
| `processOrderInPerson` | Yes | Yes | Stubbed | Foundational mapping done |
| `financeRequest` | Yes | No/unclear | Stubbed | Confirm expected business rules |
| `payBill` | Yes | Yes | Stubbed | Replace mock logic with real payment path |

## Identified gap examples

- `calculate-rental-amount` appears in service notes, but BPMN currently uses `calculate-order-amount`.
- `credentialCheck` is reused across multiple steps with different semantics.
- Some tasks exist under non-executable processes and will not emit jobs until those processes are executable.

## Definition of done for each worker

1. Job type matches BPMN `zeebe:taskDefinition type` exactly.
2. Inputs are documented (expected process variables).
3. Outputs are deterministic and documented.
4. Failure behavior is explicit (BPMN error vs technical retry).
5. One happy-path and one failure-path test evidence in Operate.
