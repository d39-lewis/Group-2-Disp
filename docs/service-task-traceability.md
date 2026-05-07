# Service Task Traceability

Source BPMN: [../DV operational BPMN V2.bpmn](../DV%20operational%20BPMN%20V2.bpmn)
*(Previously tracked against `daniel_local.bpmn` — that file is superseded.)*

---

## Executable processes

| Process ID | Participant name | Executable |
|---|---|---|
| `Process_0c6d9wt` | FixPro lts | Yes |
| `Pro` | Probuild (main) | Yes |
| `Process_1hz3vp5` | customer | Yes |
| `Process_1kwzimz` | FinTrust | Yes |
| `Process_1acsl6p` | Suppliers | **No** — workers ready but won't receive jobs until set executable |

---

## Worker implementation status

### FoundationWorkers.java — phase 1

| Job type | Process | Worker status | Key inputs | Key outputs |
|---|---|---|---|---|
| `processOrder` | Pro | **Implemented** | `customerId`, `orderTotal`, `orderChannel` | `orderId`, `orderCaptured`, `orderStatus` |
| `financeRequest` | Pro | **Implemented** | `orderTotal`, `financeInstallments` | `financeRequestId`, `financeApproved` (≤£10k auto) |
| `payBill` | FinTrust | **Implemented** | `orderTotal`, `financeRequestId` | `paymentStatus`, `transactionId`, `financeConfirmation` |

### ProBuildWorkers.java

| Job type | Process | Worker status | Key inputs | Key outputs |
|---|---|---|---|---|
| `calculate-rental-amount` | Pro | **Implemented** | `unitPrice`, `rentalDays`, `quantity` | `orderTotal`, `itemCount` |
| `receive-card-payment` | Pro | **Implemented** | `orderTotal` | `paymentSuccessful` (**string** "yes"/"no"), `transactionId` |
| `printReciept` | Pro | **Implemented** | `orderId`, `orderTotal` | `receiptPrinted`, `receiptReference` |
| `POSTransaction` | Pro | **Implemented** | `orderTotal`, `paymentMethod` | `posTransactionId`, `transactionComplete` |
| `credentialCheck` | Pro | **Implemented** | `customerId` | `credentialsValid`, `membershipActive` |
| `updateMembership` | Pro | **Implemented** | `customerId`, `membershipType` | `membershipUpdated` |
| `membershipConfimed` | Pro | **Implemented** | `customerId` | `membershipConfirmed` |
| `tradeDatabase` | Pro | **Implemented** | `businessId` / `abn` | `tradeAccountActive`, `discountRate` |
| `Queue` | Pro | **Implemented** | `customerId`, `isMember` | `queuePosition`, `estimatedWaitMinutes` |
| `queueEmail` | Pro | **Implemented** | `customerEmail`, `queuePosition` | `emailSent` |
| `paymentDecline` | Pro | **Implemented** | `customerId`, `declineReason` | `declineEmailSent` |
| `shipTool` | Pro | **Implemented** | `toolId`, `orderId`, `deliveryAddress` | `shipmentId`, `trackingNumber` |
| `auditID` | Pro | **Implemented** | `customerId`, `idDocumentType` | `idVerified`, `auditPassed` |
| `calculate-purchase-amount` | Pro | **Implemented** | `unitPrice`, `quantity` | `orderTotal`, `itemCount` |
| `membership` | Pro | **Implemented** | `customerId`, `membershipType` | `membershipValid`, `membershipId`, `membershipExpiry` |
| `financeDuration` | Pro | **Implemented** | `financeInstallments` (int or String) | `financeDurationMonths`, `financeEndDate`, `monthlyDueDate` |
| `customerAddress` | Pro | **Implemented** | `customerId`, `orderType` | `deliveryAddress`, `addressVerified`, `postcode` |

### ToolRentalWorkers.java

| Job type | Process | Worker status | Key inputs | Key outputs |
|---|---|---|---|---|
| `rentalExpiry` | Pro | **Implemented** | `rentalId`, `rentalEndDate` | `rentalExpired`, `daysOverdue`, `lateFee` |
| `returnRental` | Pro | **Implemented** | `rentalId`, `toolCondition`, `depositAmount` | `rentalClosed`, `depositRefunded`, `lateFee` |
| `depositPartialRefund` | Pro | **Implemented** | `rentalId`, `depositAmount`, `damageDeduction` | `refundAmount`, `refundProcessed` |
| `damageCost` | Pro | **Implemented** | `toolId`, `damageLevel` ("minor"/"major"/"write-off") | `repairCost`, `damageCategory` |
| `maintenaceLog` | Pro | **Implemented** | `toolId`, `maintenanceType` | `maintenanceLogged`, `logId` |
| `calculate-installment-amount` | Pro | **Implemented** | `orderTotal`, `installments` | `installmentAmount`, `totalWithInterest` |
| `installmentComplete` | Pro | **Implemented** | `financeId`, `totalInstallments`, `paidInstallments` | `allInstallmentsPaid` |
| `sendFixPro` | Pro | **Implemented** | `toolId`, `damageReport` | `sentToFixPro`, `serviceReferenceNumber` — also publishes Zeebe message `sendFixPro` |

### InventoryWorkers.java

| Job type | Process | Worker status | Key inputs | Key outputs |
|---|---|---|---|---|
| `IMS` | Pro | **Implemented** | `productId`, `changeType`, `quantity` | `imsUpdated`, `currentStock` |
| `routineIMSUpdate` | Pro | **Implemented** | `productId`, `stockLevel` | `imsUpdated` |
| `IMSUpdate` | Pro | **Implemented** | `productId`, `newStockLevel` | `imsUpdated` |
| `deductionIMS` | Pro | **Implemented** | `productId`, `deductQty` | `imsDeducted`, `newStockLevel` |
| `warehouse.IMS` | Pro | **Implemented** | `productId`, `warehouseId`, `quantity` | `warehouseImsUpdated`, `warehouseStock` |
| `fetchDelivery` | Pro | **Implemented** | `supplierId`, `orderId` | `deliveryStatus`, `estimatedArrival` |
| `Delivery` | FixPro lts | **Implemented** | `orderId` | `deliveryScheduled`, `courierReference` |
| `goodsDelivery` | Suppliers | **Implemented** (when Suppliers enabled) | `orderId` | `goodsDelivered`, `deliverySignature` |
| `servicedTools` | FixPro lts | **Implemented** | `toolId`, `serviceReferenceNumber` | `toolServiced` |
| `updateStock` | Pro | **Implemented** | `productId`, `stockDelta`, `currentStock` | `imsUpdated`, `newStockLevel`, `updateReference` |
| `customerIMSAvail` | Pro | **Implemented** | `productId`, `requestedQty` | `available`, `availableQty`, `expectedRestockDate` |
| `shipGoods` | Pro | **Implemented** | `orderId`, `deliveryAddress` | `goodsShipped`, `shipmentReference`, `estimatedDelivery` |

### FinTrustWorkers.java

| Job type | Process | Worker status | Key inputs | Key outputs |
|---|---|---|---|---|
| `6Installment` | FinTrust | **Implemented** | `orderTotal` | `monthlyAmount`, `totalWithInterest`, `financeInstallments=6` |
| `12Installment` | FinTrust | **Implemented** | `orderTotal` | `monthlyAmount`, `totalWithInterest`, `financeInstallments=12` |
| `financeEmail` | FinTrust | **Implemented** | `customerEmail`, `monthlyAmount`, `installmentNumber` | `emailSent` |
| `financeConfirmation` | FinTrust | **Implemented** | `financeRequestId` | `financeConfirmation` |
| `installmentCompletion` | FinTrust | **Implemented** | `financeRequestId` | `financeCompleted` |

---

## Unimplemented BPMN service tasks (gaps — last checked 2026-05-07)

All service task types in `DV operational BPMN V2.bpmn` have corresponding `@JobWorker` implementations. No open gaps.

---

## Known variable contract issues

| Issue | Worker affected | Status |
|---|---|---|
| `financeInstallments` arrives as String from `Website.form`; workers cast to Number | `financeEmail` | **Fixed** — `financeEmail` now safely parses String or Number |
| `damageReport.form` wrote `damageExtent` ("minor"/"moderate"/"severe"); worker reads `damageLevel` ("minor"/"major"/"write-off") | `damageCost` | **Fixed** — form key changed to `damageLevel`, values aligned to "minor"/"major"/"write-off" |
| `paymentSuccessful` is a String "yes"/"no"; BPMN gateway must use `= "yes"` not `= true` | `receive-card-payment` | Verify gateway expressions in BPMN Modeler |

---

## Definition of done for each worker

1. Job type matches BPMN `zeebe:taskDefinition type` exactly (case-sensitive).
2. Inputs documented above as key inputs.
3. Outputs are deterministic and documented.
4. Failure behavior explicit: BPMN error code (from the four defined in BPMN) or technical retry.
5. One happy-path and one failure-path test evidence in Operate.
6. This table updated with final status.
