# Forms — UX Design Rationale

**Project:** ProBuild Supplies Ltd Process Automation
**Module:** UFCFAF-30-3 Development of Information Systems Project

This document explains the design decisions behind each Camunda 8 form in the `forms/` directory: who uses it, what UX techniques were applied, what validation rules enforce business requirements, and what process variables it writes for downstream workers and gateways.

---

## Design Principles Applied Across All Forms

| Principle | Implementation |
|---|---|
| **Progressive disclosure** | Conditional fields appear only when relevant (e.g. repair cost only when Service Type = Repaired) |
| **Readonly display** | System-generated or pre-filled values shown read-only to prevent data corruption |
| **Required field validation** | All fields critical to downstream workers or gateways are marked `required: true` |
| **Digital signoff** | Where a physical signature would appear, a text field accepts full name as digital signature — practical for process automation context |
| **Consistent layout** | 16-column grid used throughout; related fields share rows; section headings break forms into logical groups |

---

## 1. Website.form

**BPMN link:** User task "customer navigate and purchase from the ProBuild website" in `Process_1hz3vp5` (customer pool)
**Actor:** Customer (self-service, external)

### Purpose
The primary customer portal. A single form covers three distinct journeys — purchase, tool hire, and Trade Card membership — using nested field groups and conditional visibility. This avoids three separate forms and keeps the customer experience unified.

### Key UX decisions
- **Radio group `mainSelection`** (purchase / hire / membership) acts as a top-level navigator — only the relevant section expands, hiding all other content. This prevents form overload and guides the customer through a single coherent journey.
- **Nested payment group** within hire and purchase sections contains card details (card number, expiry, CVV) behind a payment method radio — card fields only appear when "Card" is selected, not for cash.
- **Trade Card registration sub-section** includes address fields and card payment inline, so new members can sign up and pay in one step.
- **`isMember` checkbox** at the top feeds the `Queue` worker (`queuePosition = 1` for members vs `5` for non-members), directly affecting the business queue logic.

### Validation
- `isMember`, `mainSelection` — required; drives all downstream gateway routing
- Card fields required only when payment method = card (conditional required)
- Hire duration select required — feeds `financeDuration` and `calculate-rental-amount` workers

### Variable contract
| Variable written | Read by |
|---|---|
| `isMember` | `Queue` worker, membership gateway |
| `mainSelection` | Gateway routing (hire / purchase / membership paths) |
| `purchasePayment` / `hirePayment` | Payment gateway |
| `financeInstallments` ("6" / "12") | `financeDuration`, `6Installment`, `12Installment` workers |

---

## 2. audit.form

**BPMN link:** User task "complete audit log" (×3 instances across Pro process)
**Actor:** Warehouse manager / compliance officer

### Purpose
A dual-mode audit form covering both daily operational checks and weekly stock counts. Switching the audit type changes the visible sections and required fields — one form serves two distinct audit workflows.

### Key UX decisions
- **`auditType` select** ("Daily Check" / "Weekly Count") switches between mode-specific sections using conditional visibility. This dual-mode design eliminates the need for two separate forms.
- **Calculated `variance` field** (readonly) automatically computes `actualQuantity - expectedQuantity`. Staff see the discrepancy immediately without manual arithmetic, reducing transcription errors.
- **Repeatable checklist section** for daily items allows auditors to add as many items as needed — supports variable-length inspection lists.
- **`followUpRequired` field** directly feeds any downstream escalation gateway.

### Validation
- `auditType`, `auditorName`, `auditDate` — always required (identifies the record)
- `expectedQuantity` / `actualQuantity` — required in Weekly Count mode
- `auditorSignature` — required; ensures accountability

### Variable contract
| Variable written | Read by |
|---|---|
| `auditId` | Audit reference in downstream tasks |
| `followUpRequired` | Escalation gateway (if present in BPMN) |
| `auditType` | Process routing |

---

## 3. damageReport.form

**BPMN link:** User task "complete damage report" in Pro process
**Actor:** ProBuild staff member reporting damage; supervisor for sign-off

### Purpose
Formal incident report for damaged property or tools. Includes dual-signature block (reporter + supervisor), directly reflecting the business requirement that damage reports need management authorisation.

### Key UX decisions
- **Two-tier signoff**: Reporter signs first; a separate supervisor section (name + signature + date) captures management review. This models a real compliance workflow.
- **`damageLevel` select** ("Minor" / "Major" / "Write-Off") uses exactly the values consumed by the `damageCost` worker (`minor`→£50, `major`→£250, `write-off`→£800) — no mapping step needed.
- **`incidentNarrative` textarea** is required and distinct from the damage description, capturing how the damage occurred (liability / insurance context).

### Validation
- All reporter fields required — ensures the report is attributable
- `damageLevel` required — drives `damageCost` worker and the repair authorisation gateway
- Supervisor fields optional — manager may not always be present at time of initial report

### Variable contract
| Variable written | Read by |
|---|---|
| `damageLevel` | `damageCost` worker (determines repair cost) |
| `damageDescription` | Passed to `sendFixPro` for FixPro context |

---

## 4. PATTest.form

**BPMN link:** User task "complete test report" (×2 in FixPro process)
**Actor:** FixPro technician

### Purpose
Records the Portable Appliance Test (PAT) result for a returned hire tool. This is a legal compliance requirement in the UK for electrical equipment — the form captures test type, readings, result, and next service date to maintain the service log.

### Key UX decisions
- **`testResult` field** (pass / fail) is the key output — if "fail", the BPMN `PAT_FAIL` error boundary event is designed to catch a `ZeebeBpmnError`. This field is required because the pass/fail determination directly controls which path the process takes.
- **`nextServiceDate`** gives the tool a maintenance schedule, supporting the long-term hire inventory lifecycle.
- **`testReadings` textarea** is optional — captures numeric measurements for record-keeping without blocking the workflow for cases where readings aren't applicable.

### Validation
- `toolId`, `technicianId`, `testType`, `testDate`, `testResult` — all required
- `serviceDate`, `nextServiceDate` — required for maintenance scheduling

### Variable contract
| Variable written | Read by |
|---|---|
| `testResult` | PAT_FAIL error boundary event gateway |
| `nextServiceDate` | Maintenance log / scheduling |

---

## 5. serviceReport.form

**BPMN link:** User task "complete service report" (×5 in FixPro process)
**Actor:** FixPro technician

### Purpose
Captures the technician's assessment of what work was done. The service type drives conditional fields — only relevant sections appear, keeping the form concise regardless of which path the tool took.

### Key UX decisions
- **`serviceType` select** ("Maintenance" / "Repaired" / "Decommissioned") drives three distinct conditional sub-sections:
  - Repaired: shows description of repairs + cost field (£ prefixed number input)
  - Decommissioned: shows mandatory justification textarea
  - Maintenance: no additional fields (routine service)
- **Conditional `required`** on the decommission reason uses a FEEL expression (`=serviceType = "decommissioned"`) — validation is dynamically applied, so staff aren't blocked unless the field actually applies.
- **`repairCost` number field** with `$` prefix adorner gives immediate financial context; this value flows to the deposit/refund calculation workers.

### Validation
- `toolName`, `toolId`, `serviceType`, `technicianName`, `serviceDate`, `signature` — always required
- `decommissionReason` required only when serviceType = "decommissioned"

### Variable contract
| Variable written | Read by |
|---|---|
| `serviceType` | Routing in FixPro subprocess |
| `repairCost` | `depositPartialRefund` worker |
| `signature` | Audit trail |

---

## 6. serviceLabel.form

**BPMN link:** User task "complete service label" in FixPro process
**Actor:** FixPro technician

### Purpose
Generates the physical label attached to a tool after servicing. Includes a repeatable parts-replaced section — technicians can log multiple parts in a single form submission.

### Key UX decisions
- **Repeatable section** for parts replaced (part number, description, quantity) supports variable-length parts lists without requiring a separate form per part. Each repetition is a self-contained row.
- **`returnId` field** creates a linkage between the service label and the original hire contract, enabling traceability if a customer queries a past repair.
- Fields are kept concise — this is an operational form, not a report; technicians complete it quickly at end of service.

### Validation
- `returnDate`, `returnId`, `toolId`, `serviceCompletionDate` — required (core record identity)
- `partQuantity` minimum = 1 when a part row is added

---

## 7. handoverForm.form

**BPMN link:** User task "complete the tool handover" in FixPro process
**Actor:** ProBuild supervisor + FixPro driver (joint completion)

### Purpose
Records the physical handover of tools between ProBuild and the FixPro collection driver. Two-party signoff (supervisor + driver) captures legal responsibility transfer.

### Key UX decisions
- **Dual signoff** (supervisor signature + driver signature) mirrors real logistics practice — both parties confirm the handover, creating a dispute-free record.
- **`faultDescription` field** allows staff to flag any faults noted at handover that weren't in the original damage report — an important safety net before tools enter the service cycle.
- **`serviceType` field** on this form gives FixPro the context for what work is required before the tool arrives at their workshop.

### Validation
- `supervisorName`, `driverName`, `supervisorSignature`, `driverSignature` — required (both parties must sign)
- `toolId`, `serviceType`, `conditionNotes` — required

---

## 8. barcode.form

**BPMN link:** User task "scan barcode" in Pro process (supplier delivery receipt)
**Actor:** Warehouse staff member

### Purpose
Displays delivered item quantities to the warehouse operative scanning goods in. All fields are **read-only** — this is a display form showing data pre-populated from the process (supplier delivery manifest), not a data entry form.

### Key UX decisions
- **All fields readonly** — the form acts as a digital delivery note. The warehouse operative confirms receipt by clicking Complete, with the quantities visible for physical verification against the actual goods.
- **Conditional visibility** (`hide: =not(X > 0)`) — item rows only appear for quantities > 0. A delivery of only cement mixers shows only the cement mixer row, keeping the display uncluttered.
- **11 product types** cover the full ProBuild inventory range (equipment: mixers, scaffolding, washers, cutters, drills; materials: timber, insulation, plasterboard, power tools, fasteners, adhesives).

### Validation
None — all fields are readonly. Completion of the user task itself constitutes the receipt confirmation.

---

## 9. decom.form

**BPMN link:** User task "flag for decommission" (×2 in Pro process)
**Actor:** Warehouse manager / senior staff

### Purpose
Formal decommissioning record for tools or assets that are beyond economic repair or end-of-life. Requires detailed justification and sign-off before an asset can be written off.

### Key UX decisions
- **`detailedJustification` textarea** is required — ensures the decision to write off an asset is documented and auditable.
- **`finalSignoffSignature`** (typed full name) represents management authorisation; without it, a decommission cannot be recorded.
- **`toolType` field** distinguishes asset categories (hire tool vs warehouse equipment vs vehicle) to support asset register updates.

### Validation
- `assetName`, `assetId`, `toolType`, `detailedJustification`, `finalSignoffName`, `finalSignoffSignature`, `finalSignoffDate` — all required

---

## 10. discremicyReport.form

**BPMN link:** User task "generate discrepancy report" in Pro process
**Actor:** Warehouse staff / stock controller

### Purpose
Records inventory discrepancies found during stock checks — items that are missing, damaged, or in unexpected condition. Feeds the warehouse reconciliation process.

### Key UX decisions
- **`expectedCondition` / `actualCondition` pair** explicitly captures the delta rather than just "something is wrong", making the discrepancy concrete and actionable.
- **`potentialImpact` field** allows staff to flag downstream consequences (e.g. a missing tool may affect upcoming hire bookings), supporting prioritised resolution.
- **Date and time of discovery** fields separate from the filing date — important for incident investigation to establish when the discrepancy actually occurred.

### Validation
- `filedBy`, `dateFiled`, `dateDiscrepancyFound`, `expectedCondition`, `actualCondition`, `detailedDescription` — required

---

## 11. stockLevel.form

**BPMN link:** User task "display stock with levels" in Pro process
**Actor:** Warehouse manager (read-only review)

### Purpose
Displays current IMS stock levels across the full ProBuild inventory at a point in time. Like the barcode form, this is a **display-only** form — all 11 fields are readonly, populated from process variables output by IMS workers.

### Key UX decisions
- **Two sections** — Equipment (6 product types) and Materials (5 product types) — mirror the physical warehouse layout.
- **"Acknowledge" button** (checkbox) at the bottom creates a lightweight confirmation that the manager has reviewed the stock levels, without requiring any data entry.
- All values fed directly from `IMS` worker outputs, ensuring the displayed levels match the system of record in real time.

---

## 12. toolInspection.form

**BPMN link:** Not yet linked to a BPMN user task (pending BPMN update)
**Actor:** Warehouse staff (tool return counter)

### Purpose
Records the condition of tools returned from hire. The defect/no-defect radio drives the damage reporting flow.

### Key UX decisions
- **`hasDefects` radio** (yes/no) with conditional `damageDescription` textarea — the description is only required if defects are present (FEEL expression: `=hasDefects = "yes"`). This prevents unnecessary friction for clean returns.
- **`returnedTools` checklist** with five common hire tools lets staff tick which tools were physically returned without free-text entry (reduces transcription errors).

### Validation
- `hasDefects` required (drives damage flow)
- `damageDescription` required only when `hasDefects = "yes"`

---

## 13. in-person hiring.form

**BPMN link:** Not yet linked to a BPMN user task (pending BPMN update)
**Actor:** ProBuild counter staff (assisted hire, in-store)

### Purpose
Counter-staff form for in-person tool hire requests. Staff complete this on behalf of the customer at the hire desk.

### Key UX decisions
- **Checklist of 5 common hire tools** rather than a free-text field — ensures only hireable inventory items are selected and simplifies data entry at a busy counter.
- **`hasTradeCard` checkbox** — immediately flags Trade Card members for discount application without requiring staff to look up membership separately.
- **`hirePeriod` select** (half-day, 1 day, 1 week, etc.) maps directly to the rental duration pricing table.

---

## 14. in-person Queue.form

**BPMN link:** Not yet linked to a BPMN user task (pending BPMN update)
**Actor:** Customer (self-service kiosk or counter staff assisted)

### Purpose
Queue registration for in-store customers waiting for counter service. Minimal fields to keep queue entry fast.

### Key UX decisions
- **Minimal required fields** (`firstName`, `lastName`, `email`, `toolsToHire`) — queue entry should take under 30 seconds.
- **`hasTradingCardMembership` radio** feeds the queue priority logic (members get lower queue positions).
- **Email field** enables the `queueEmail` worker to notify the customer when their position is reached.

---

## 15. qualityInpect.form

**Form ID:** `qualityInspect`
**BPMN link:** "quality inspection check" user task (warehouse/goods receipt path)
**Actor:** Warehouse staff + supervisor

### Purpose
Structured quality inspection checklist for incoming goods or returned tools. Replaces a `bpmn:manualTask` that previously passed through silently, leaving the downstream `qualityInspect` gateway with no variable to evaluate.

### Key UX decisions
- **Four checklist items** each with a status select and comments field — covers condition, completeness, safety, and documentation.
- **`overallResult` select** ("pass"/"fail") drives the gateway directly. Positioned prominently after the checklist so the inspector makes a considered judgement rather than defaulting.
- **Dual signoff** (inspector + supervisor signatures with dates) — mirrors `damageReport.form` for consistency across inspection workflows.
- **`reasonForFailure` and `defectsDescription` text areas** provide the paper trail needed if goods are rejected or a tool is flagged for repair.

### Validation
`overallResult` is required — the gateway will error if left blank. Inspector signature and date are required; supervisor fields are recommended but not mandatory to allow single-person inspections in low-staffing scenarios.

### Variable contract
| Variable | Direction | Notes |
|---|---|---|
| `overallResult` | Output | `"pass"` or `"fail"` — read by `qualityInspect` gateway |
| `inspectorName` | Output | Audit trail |
| `defectsDescription` | Output | Passed to downstream repair/rejection tasks |
| `supervisorSignature` | Output | Compliance signoff |

---

## 16. toolRepairs.form

**Form ID:** `toolRepairs`
**BPMN link:** "what work is required of the tool?" user task (FixPro maintenance path)
**Actor:** Workshop / maintenance staff

### Purpose
Captures the repair decision for a tool that has been returned or flagged for servicing. Replaces a `bpmn:manualTask` that previously left the `toolRepairs` gateway unable to evaluate its condition.

### Key UX decisions
- **`requestedAction` select** ("repair"/"maintenace"/"dispose") maps directly to the three gateway branches. The BPMN uses "maintenace" (matching the typo in the gateway condition) so the form value must match exactly.
- **`urgencyLevel` select** gives the workshop a priority signal without adding a gateway — informational only.
- **Requester section** (name, employee ID, department) provides accountability for the disposal/repair decision.

### Validation
`requestedAction` and `toolName` are required. `requestReason` is required to ensure disposals are justified.

### Variable contract
| Variable | Direction | Notes |
|---|---|---|
| `requestedAction` | Output | `"repair"` / `"maintenace"` / `"dispose"` — read by `toolRepairs` gateway |
| `toolName` | Output | Identifies the asset |
| `urgencyLevel` | Output | Informational priority flag |

---

## 17. read the service report.form

**Form ID:** `read-the-service-report-09meolu`
**BPMN link:** "read the service report" user task (post-service tool review path)
**Actor:** Supervisor / warehouse manager

### Purpose
After a tool has been serviced by FixPro, a supervisor reviews the service report and decides whether the tool needs replacing. Replaces a `bpmn:task` that previously passed through silently, leaving the `replaceTools` gateway with no variable.

### Key UX decisions
- **`replacing` select** ("yes"/"no") drives the gateway. Placed in the top-level form (not inside the repeatable section) so it's always visible and required before submission.
- **Repeatable tool info section** allows the form to cover multi-tool service reports in a single submission.
- **Replacement justification fields** (reason, last service date, issue description, estimated cost) build the case for a replace decision — discourages casual replacements.
- **Dual approval** (supervisor name + approval status + signature) mirrors the handover form pattern.

### Validation
`replacing` is required. `supervisorName` and `approvalStatus` are required for the approval record.

### Variable contract
| Variable | Direction | Notes |
|---|---|---|
| `replacing` | Output | `"yes"` or `"no"` — read by `replaceTools` gateway |
| `approvalStatus` | Output | Records formal approval decision |
| `supervisorSignature` | Output | Compliance signoff |

---

## 18. detect for discrepency.form

**Form ID:** `detect-for-discrepency-1wc1ur3`
**BPMN link:** "detect for discrepency" user task (warehouse audit / goods receipt path)
**Actor:** Stock controller / warehouse staff

### Purpose
Formal report when a discrepancy is detected between expected and actual stock or delivery contents. The `discrepencies` output drives the downstream gateway to route to the resolution path or continue normally.

### Key UX decisions
- **`discrepencies` select** ("yes"/"no") is the gateway-driving field, positioned early in the form so staff are forced to make the binary decision before entering detail.
- **Repeatable discrepancy items section** (`discrepencies` array) allows multiple line items to be reported in a single submission.
- **Declaration checkbox + signature** provide a formal attestation that the report is accurate — important for supplier dispute resolution.
- **Reporter info section** (name, email, department, date) ensures the report is traceable to a specific individual.

### Validation
`discrepencies` (the select), `reporterFullName`, and `reporterSignature` are required. The declaration checkbox must be ticked before submission.

### Variable contract
| Variable | Direction | Notes |
|---|---|---|
| `discrepencies` | Output | `"yes"` or `"no"` — read by discrepancy gateway |
| `reporterFullName` | Output | Audit trail |
| `reporterSignature` | Output | Formal attestation |

---

## Summary Table

| Form | Actor | Editable fields | Readonly fields | Conditional logic | Multi-signoff |
|---|---|---|---|---|---|
| Website.form | Customer | 15+ | 0 | Yes (3 major sections) | No |
| audit.form | Manager | 12+ | 1 (variance) | Yes (audit type) | No |
| damageReport.form | Staff + Supervisor | 13 | 0 | No | Yes (2-tier) |
| PATTest.form | Technician | 8 | 0 | No | No |
| serviceReport.form | Technician | 7 | 0 | Yes (service type) | No |
| serviceLabel.form | Technician | 8 + repeatable | 0 | No | No |
| handoverForm.form | Supervisor + Driver | 8 | 0 | No | Yes (2-tier) |
| barcode.form | Warehouse staff | 0 | 12 | Yes (qty > 0) | No |
| decom.form | Manager | 9 | 0 | No | No |
| discremicyReport.form | Stock controller | 8 | 0 | No | No |
| stockLevel.form | Manager | 1 (acknowledge) | 11 | No | No |
| toolInspection.form | Warehouse staff | 2 + checklist | 0 | Yes (hasDefects) | No |
| in-person hiring.form | Counter staff | 4 + checklist | 0 | No | No |
| in-person Queue.form | Customer / staff | 5 | 0 | No | No |
| qualityInpect.form | Warehouse staff + supervisor | 10+ checklist | 0 | No | Yes (2-tier) |
| toolRepairs.form | Workshop staff | 7 | 0 | No | No |
| read the service report.form | Supervisor | 12+ repeatable | 0 | Yes (replacing) | Yes (2-tier) |
| detect for discrepency.form | Stock controller | 8+ repeatable | 0 | No | No |
