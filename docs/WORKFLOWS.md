# End-to-End Workflows

## WF-001: Property → Room → Operational Availability
1. Owner creates property (`POST /api/properties`).
2. Owner adds room with unique code per property (`POST /api/properties/{id}/rooms`), defaulting to `AVAILABLE`.
3. Owner may toggle non-occupied room between `AVAILABLE` and `MAINTENANCE` (`POST /api/properties/{propertyId}/rooms/{roomId}/status`). Attempting to alter an `OCCUPIED` room returns `409 Conflict`.

## WF-002: Tenant → Draft Contract → Activate → Move In
1. Owner creates tenant (`POST /api/tenants`).
2. Owner creates draft contract (`POST /api/contracts`), snapshotting `monthlyRent` and `depositAmount`.
3. Owner activates contract (`POST /api/contracts/{id}/activate`). Inside one `@Transactional` boundary: validates `DRAFT` state, checks date overlap against `ACTIVE` contracts on the same room, checks `RoomStatus.AVAILABLE`, sets room `OCCUPIED` and contract `ACTIVE`.

## WF-003: Tariffs → Meter Readings → Invoice Generation & Issue
1. Owner configures electricity/water unit tariffs (`POST /api/utility-tariffs`).
2. Owner records period readings (`POST /api/meter-readings`). Service verifies no duplicate period, no newer period already exists, and `currentReading >= previousReading`, then computes `charge = (current - previous) * price` rounded HALF_UP.
3. Owner generates invoices (`POST /api/invoices/generate`) for active contracts overlapping the billing month.
4. Owner issues invoice (`POST /api/invoices/{id}/issue`) transitioning `DRAFT` → `ISSUED` and emitting an in-app notification.

## WF-004: Manual Payment → Allocation → Balance Update
1. Operator enters manual payment (`POST /api/invoices/{id}/payments`) with `amount`, `method` (`CASH` or `BANK_TRANSFER`), `reference`, and client `idempotencyKey`.
2. Backend checks idempotency key; rejects mismatched reuse (`409`) or overpayment (`400`), increments `amountPaid`, updates status (`PARTIALLY_PAID` or `PAID`), and emits notification.

## WF-005: Maintenance Request → Assign → Resolve → Close
1. Owner creates ticket for a room (`POST /api/maintenance-tickets`, status `OPEN`).
2. Owner assigns technician (`POST /api/maintenance-tickets/{id}/assign` → `ASSIGNED`).
3. Owner updates progress (`IN_PROGRESS`, optional `ON_HOLD`), resolves with `actualCost` & `resolutionNotes` (`RESOLVED`), closes (`CLOSED`), or reopens (`REOPENED`). Invalid transitions fail with `409 Conflict`.

## WF-006: Terminate Contract → Settle Balance & Deposit → Handover → Release Room
1. Owner ensures all draft/issued invoices for the active contract are settled (`balanceDue == 0`).
2. Owner submits termination (`POST /api/contracts/{id}/terminate`) with `terminatedDate`, `depositDeducted`, optional final electricity/water readings, and checklist notes.
3. Backend atomically transitions contract `ACTIVE` → `TERMINATED`, releases room `OCCUPIED` → `AVAILABLE`, saves `HandoverRecord` (`depositRefunded = depositHeld - depositDeducted`), and records a notification.
