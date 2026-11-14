# Business Rules Catalog

| Rule ID | Domain | Rule Description | Enforced In |
|---|---|---|---|
| **BR-001** | Organization Scope | Every domain entity belongs to one `organization_id` derived solely from authenticated `RmsUserPrincipal`, never client payloads. | All `*Service` & `*Repository` |
| **BR-002** | Exclusive Occupancy | Only one `ACTIVE` contract may occupy a room for overlapping date ranges (`start_date <= existing.end_date AND end_date >= existing.start_date`); room must be `AVAILABLE` at activation. | `ContractService.activate`, `Room.markOccupied` |
| **BR-003** | Room Manual Status | An `OCCUPIED` room cannot be manually switched to `AVAILABLE`, `MAINTENANCE`, or `INACTIVE`; it releases only via contract termination. | `Room.setOperationalStatus` |
| **BR-004** | Price & Deposit Snapshot | Creating a contract captures `room.monthlyRent` and `room.depositAmount` so future room price edits do not mutate existing contracts. | `ContractService.create` |
| **BR-005** | Configurable Tariffs | Utility prices are entered by the organization owner with an `effectiveFrom` date; no government tariff is hardcoded. | `UtilityService.createTariff` |
| **BR-006** | Monotonic Meter Readings | A room/utility reading must be `>= previousReading`, cannot duplicate an existing `(room, utilityType, billingPeriod)`, and cannot be backdated before an already recorded newer period. | `UtilityService.createReading`, `V3` constraints |
| **BR-007** | Deterministic VND Billing | Monetary values use `BigDecimal` rounded to integer đồng (`RoundingMode.HALF_UP`). One invoice per `(organization, contract, billingPeriod)`. | `UtilityService`, `BillingService`, `RentalInvoice` |
| **BR-008** | Issued Invoice Integrity | Only `DRAFT` invoices can be issued; once issued, amounts cannot be silently overwritten by re-running generation. | `BillingService`, `RentalInvoice` |
| **BR-009** | Manual Payment Idempotency | Payments require positive `amount <= balanceDue` and a unique `idempotencyKey`. Identical retries return the original result; mismatched retries fail with `409 Conflict`. | `BillingService.recordPayment`, `uk_payment_org_idempotency` |
| **BR-010** | Contract Termination & Deposit Settlement | Termination requires `ACTIVE` contract, zero `DRAFT` invoices, zero unpaid invoice balance, and `0 <= depositDeducted <= depositHeld`. `depositRefunded = depositHeld - depositDeducted`. Room transitions `OCCUPIED` → `AVAILABLE` atomically. | `ContractService.terminate`, `V4` constraints |
| **BR-011** | Maintenance Ticket State Machine | Ticket transitions follow `TicketStatus.canTransitionTo`; illegal jumps (e.g. `OPEN` → `RESOLVED` directly) fail with `409 Conflict`. | `TicketStatus`, `MaintenanceTicket` |
