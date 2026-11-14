# REST API Specification

All endpoints except `/api/auth/csrf`, `/api/auth/register`, `/api/auth/login`, and `/actuator/health/**` require an authenticated `RMSSESSION` cookie. All state-changing requests (`POST`/`PUT`/`PATCH`/`DELETE`) require a valid `X-XSRF-TOKEN` header matching the `XSRF-TOKEN` cookie.

## Authentication & Session
- `GET /api/auth/csrf` — Returns `{ "token": "...", "headerName": "X-XSRF-TOKEN" }` and sets `XSRF-TOKEN` cookie.
- `POST /api/auth/register` — `{ email, password (12..72), fullName, organizationName }` → `201 Created` (or `409` on duplicate email).
- `POST /api/auth/login` — `{ email, password }` → `200 OK` `{ userId, organizationId, organizationName, email, role }`.
- `GET /api/auth/me` — Returns current authenticated `LoginResponse` or `401`.
- `POST /api/auth/logout` — Invalidates session → `204 No Content`.

## Properties & Rooms
- `GET /api/properties` / `POST /api/properties` (`{ name, address }`)
- `GET /api/properties/{propertyId}`
- `GET /api/properties/{propertyId}/rooms` / `POST /api/properties/{propertyId}/rooms` (`{ roomCode, areaSquareMeters, capacity, monthlyRent, depositAmount }`)
- `POST /api/properties/{propertyId}/rooms/{roomId}/status` — `{ status: "AVAILABLE" | "MAINTENANCE" | "INACTIVE" }` (`409` if `OCCUPIED`).

## Tenants, Contracts & Handover
- `GET /api/tenants` / `POST /api/tenants` (`{ fullName, phone?, email? }`)
- `GET /api/contracts` / `POST /api/contracts` (`{ roomId, tenantId, startDate, endDate }`)
- `POST /api/contracts/{contractId}/activate` — Transitions `DRAFT` → `ACTIVE`, sets room `OCCUPIED`.
- `POST /api/contracts/{contractId}/terminate` — `{ terminatedDate, depositDeducted?, finalElectricityReading?, finalWaterReading?, checklistNotes? }` → `HandoverResponse`.
- `GET /api/contracts/{contractId}/handover` — Returns saved `HandoverResponse`.

## Utilities, Invoices & Payments
- `GET /api/utility-tariffs` / `POST /api/utility-tariffs` (`{ utilityType, pricePerUnit, effectiveFrom }`)
- `GET /api/meter-readings?billingPeriod=YYYY-MM` / `POST /api/meter-readings` (`{ roomId, utilityType, billingPeriod, currentReading }`)
- `GET /api/invoices?billingPeriod=YYYY-MM` / `POST /api/invoices/generate` (`{ billingPeriod }`)
- `POST /api/invoices/{invoiceId}/issue`
- `POST /api/invoices/{invoiceId}/payments` (`{ amount, method, reference, idempotencyKey }`)

## Maintenance & Notifications
- `GET /api/maintenance-tickets` / `POST /api/maintenance-tickets` (`{ roomId, title, description, priority, estimatedCost }`)
- `POST /api/maintenance-tickets/{ticketId}/assign` (`{ assigneeName }`)
- `POST /api/maintenance-tickets/{ticketId}/status` (`{ status, actualCost?, resolutionNotes? }`)
- `GET /api/notifications` / `POST /api/notifications/{notificationId}/read`
