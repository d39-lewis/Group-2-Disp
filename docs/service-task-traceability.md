# Service Task Traceability

Source BPMN: [../daniel_local.bpmn](../daniel_local.bpmn)
Source task notes: [../service+task+descriptions.doc](../service+task+descriptions.doc)

---

## Executable processes

| Process ID | Participant name | Executable |
|---|---|---|
| `Process_0c6d9wt` | FixPro lts | Yes |
| `Process_ProBuildRetail` | PROBUILD RETAIL SALES PROCESS | Yes |
| `Pro` | Probuild (main) | Yes |
| `Process_1hz3vp5` | customer | Yes |
| `Process_1kwzimz` | FinTrust | **No** — workers ready but won't receive jobs until set executable |

---

## Worker implementation status

### FoundationWorkers.java — phase 1 (existing, enhanced)

| Job type | Process | Worker status | Key inputs | Key outputs |
|---|---|---|---|---|
| `processOrder` | Pro | **Implemented** | `customerId`, `orderTotal`, `orderChannel` | `orderId`, `orderCaptured`, `orderStatus` |
| `processOrderInPerson` | Pro | **Implemented** | `customerId`, `membershipId` | `orderId`, `inPersonOrderCaptured`, `orderStatus` |
| `financeRequest` | Pro | **Implemented** | `orderTotal`, `financeInstallments` | `financeRequestId`, `financeApproved` (≤£10k auto) |
| `payBill` | FinTrust | **Implemented** | `orderTotal`, `financeRequestId` | `paymentStatus`, `transactionId`, `financeConfirmation` |

### RetailWorkers.java

| Job type | Process | Worker status | Key inputs | Key outputs |
|---|---|---|---|---|
| `retail.stockCheck` | ProBuildRetail | **Implemented** | `productId` | `stockAvailable`, `stockLevel` |
| `retail.onlineStockCheck` | ProBuildRetail | **Implemented** | `productId` | `stockAvailable`, `stockLevel` |
| `retail.processCardPayment` | ProBuildRetail | **Implemented** | `orderTotal` | `paymentStatus`, `transactionId` |
| `retail.processOnlinePayment` | ProBuildRetail | **Implemented** | `orderTotal` | `paymentStatus`, `transactionId` |
| `retail.processCCPayment` | ProBuildRetail | **Implemented** | `orderTotal` | `paymentStatus`, `transactionId` |
| `retail.createOrder` | ProBuildRetail | **Implemented** | `customerId`, `orderTotal`, `fulfilmentMethod` | `orderId`, `orderStatus` |
| `retail.confirmSelection` | ProBuildRetail | **Implemented** | `orderId` | `selectionConfirmed` |

### ProBuildWorkers.java

| Job type | Process | Worker status | Key inputs | Key outputs |
|---|---|---|---|---|
| `calculate-order-amount` | Pro | **Implemented** | `unitPrice`, `rentalDays`, `quantity` | `orderTotal`, `itemCount` |
| `receive-card-payment` | Pro | **Implemented** | `orderTotal` | `paymentSuccessful` ("yes"/"no"), `transactionId` |
| `printReciept` | Pro | **Implemented** | `orderId`, `orderTotal` | `receiptPrinted`, `receiptReference` |
| `POSTransaction` | Pro | **Implemented** | `orderTotal`, `paymentMethod` | `posTransactionId`, `transactionComplete` |
| `credentialCheck` | Pro | **Implemented** | `customerId` | `credentialsValid`, `membershipActive` |
| `updateMembership` | Pro | **Implemented** | `customerId`, `membershipType` | `membershipUpdated` |
| `membershipConfimed` | Pro | **Implemented** | `customerId` | `membershipConfirmed` |
| `tradeDatabase` | Pro | **Implemented** | `businessId` / `abn` | `tradeAccountActive`, `discountRate` |
| `membershipDiscount` | Pro | **Implemented** | `orderTotal`, `membershipType` | `discountedTotal`, `discountApplied` |
| `Queue` | Pro | **Implemented** | `customerId`, `isMember` | `queuePosition`, `estimatedWaitMinutes` |
| `queueEmail` | Pro | **Implemented** | `customerEmail`, `queuePosition` | `emailSent` |
| `paymentDecline` | Pro | **Implemented** | `customerId`, `declineReason` | `declineEmailSent` |
| `shipTool` | Pro | **Implemented** | `toolId`, `orderId`, `deliveryAddress` | `shipmentId`, `trackingNumber` |
| `auditID` | Pro | **Implemented** | `customerId`, `idDocumentType` | `idVerified`, `auditPassed` |

### ToolRentalWorkers.java

| Job type | Process | Worker status | Key inputs | Key outputs |
|---|---|---|---|---|
| `toolAvailable` | Pro | **Implemented** | `toolId` | `toolInStock`, `toolLocation` |
| `storeRetrieveTool` | Pro | **Implemented** | `toolId`, `rentalId` | `toolRetrieved`, `toolCondition` |
| `rentalExpiry` | Pro | **Implemented** | `rentalId`, `rentalEndDate` | `rentalExpired`, `daysOverdue`, `lateFee` |
| `returnRental` | Pro | **Implemented** | `rentalId`, `toolCondition`, `depositAmount` | `rentalClosed`, `depositRefunded`, `lateFee` |
| `depositPartialRefund` | Pro | **Implemented** | `rentalId`, `depositAmount`, `damageDeduction` | `refundAmount`, `refundProcessed` |
| `damageCost` | Pro | **Implemented** | `toolId`, `damageLevel` | `repairCost`, `damageCategory` |
| `maintenaceLog` | Pro | **Implemented** | `toolId`, `maintenanceType` | `maintenanceLogged`, `logId` |
| `calculate-installment-amount` | Pro | **Implemented** | `orderTotal`, `installments` | `installmentAmount`, `totalWithInterest` |
| `installmentComplete` | Pro | **Implemented** | `financeId`, `totalInstallments`, `paidInstallments` | `allInstallmentsPaid` |
| `sendFixPro` | Pro | **Implemented** | `toolId`, `damageReport` | `sentToFixPro`, `serviceReferenceNumber` |

### InventoryWorkers.java

| Job type | Process | Worker status | Key inputs | Key outputs |
|---|---|---|---|---|
| `IMS` | Pro | **Implemented** | `productId`, `changeType`, `quantity` | `imsUpdated`, `currentStock` |
| `routineIMSUpdate` | Pro | **Implemented** | `productId`, `stockLevel` | `imsUpdated` |
| `IMSUpdate` | Pro | **Implemented** | `productId`, `newStockLevel` | `imsUpdated` |
| `deductionIMS` | Pro | **Implemented** | `productId`, `deductQty` | `imsDeducted`, `newStockLevel` |
| `warehouse.IMS` | Pro | **Implemented** | `productId`, `warehouseId`, `quantity` | `warehouseImsUpdated`, `warehouseStock` |
| `warehouse.tradeDatabase` | Pro | **Implemented** | `businessId` | `tradeDatabaseUpdated` |
| `fetchDelivery` | Pro | **Implemented** | `supplierId`, `orderId` | `deliveryStatus`, `estimatedArrival` |
| `Delivery` | Pro | **Implemented** | `orderId`, `deliveryAddress` | `deliveryScheduled`, `courierReference` |
| `supplierDelivery` | Suppliers | **Implemented** | `supplierId`, `orderId` | `supplierConfirmed`, `dispatchDate` |
| `goodsDelivery` | Suppliers | **Implemented** | `orderId` | `goodsDelivered`, `deliverySignature` |
| `servicedTools` | FixPro lts | **Implemented** | `toolId`, `serviceReferenceNumber` | `toolServiced` |

### FinTrustWorkers.java

| Job type | Process | Worker status | Key inputs | Key outputs |
|---|---|---|---|---|
| `6Installment` | FinTrust | **Implemented** (process not yet executable) | `orderTotal` | `monthlyAmount`, `totalWithInterest` |
| `12Installment` | FinTrust | **Implemented** (process not yet executable) | `orderTotal` | `monthlyAmount`, `totalWithInterest` |
| `financeEmail` | FinTrust | **Implemented** (process not yet executable) | `customerEmail`, `monthlyAmount`, `installmentNumber` | `emailSent` |
| `financeConfirmation` | FinTrust | **Implemented** (process not yet executable) | `financeRequestId` | `financeConfirmation` |
| `installmentCompletion` | FinTrust | **Implemented** (process not yet executable) | `financeRequestId` | `financeCompleted` |

---

## Known gaps / open items

- `calculate-rental-amount` appears in service notes but BPMN uses `calculate-order-amount` — confirm if a separate type is needed.
- `credentialCheck` is reused for both credential validation and "display failed inputs" — consider splitting to separate types.
- `retail.processCardPayment`, `retail.processOnlinePayment`, `retail.processCCPayment` have BPMN boundary error events (`PaymentDeclined`). Workers currently complete successfully (happy path); to trigger the error path, inject a `JobClient` and call `newThrowErrorCommand` when card auth fails.
- FinTrust process (`Process_1kwzimz`) is `isExecutable="false"` — set to `true` in the BPMN to activate those workers.

## Definition of done for each worker

1. Job type matches BPMN `zeebe:taskDefinition type` exactly.
2. Inputs are documented (expected process variables).
3. Outputs are deterministic and documented.
4. Failure behavior is explicit (BPMN error vs technical retry).
5. One happy-path and one failure-path test evidence in Operate.
