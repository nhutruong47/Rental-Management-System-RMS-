# REST API Specification

All endpoints except `/api/auth/csrf`, `/api/auth/register`, `/api/auth/login`, and `/actuator/health/**` require an authenticated `RMSSESSION` cookie. All state-changing requests (`POST`/`PUT`/`PATCH`/`DELETE`) require a valid `X-XSRF-TOKEN` header matching the `XSRF-TOKEN` cookie. Role authorization is enforced via `@PreAuthorize` on every controller.

## Authentication & Session
- `GET /api/auth/csrf` — Returns `{ "token": "...", "headerName": "X-XSRF-TOKEN" }` and sets `XSRF-TOKEN` cookie.
- `POST /api/auth/register` — `{ email, password (12..72), fullName, organizationName }` → `201 Created` (`OWNER`).
- `POST /api/auth/login` — `{ email, password }` → `200 OK` `{ userId, organizationId, organizationName, email, role }`. Protected by `AuthRateLimiter` (`429 Too Many Requests` after 10 failures / 60s).
- `GET /api/auth/me` — Returns current authenticated `LoginResponse` or `401`.
- `POST /api/auth/change-password` — `{ currentPassword, newPassword (12..72) }` → `204 No Content` (any authenticated role).
- `POST /api/auth/logout` — Invalidates session → `204 No Content`.

## Members & RBAC (`OWNER`, `MANAGER` read)
- `GET /api/members` (`OWNER`, `MANAGER`) — List organization memberships.
- `POST /api/members/invite` (`OWNER`) — `{ fullName, email, password (12..72), role ("MANAGER" | "STAFF") }` → `201 Created`.
- `PATCH /api/members/{membershipId}/role` (`OWNER`) — `{ role ("OWNER" | "MANAGER" | "STAFF") }`.
- `DELETE /api/members/{membershipId}` (`OWNER`) — Removes non-self membership.

## Organization Settings & Audit Logs
- `GET /api/settings` (`OWNER`, `MANAGER`, `STAFF`) — Returns default billing due day and recurring monthly fees (`defaultInternetFee`, `defaultGarbageFee`, `defaultParkingFee`).
- `PUT /api/settings` (`OWNER`, `MANAGER`) — Updates organization billing & recurring fee defaults.
- `GET /api/audit-logs` (`OWNER`, `MANAGER`) — Returns immutable organization audit log entries.

## Properties & Rooms
- `GET /api/properties` (`OWNER`, `MANAGER`, `STAFF`) / `POST /api/properties` (`OWNER`, `MANAGER`)
- `GET /api/properties/{propertyId}` (`OWNER`, `MANAGER`, `STAFF`)
- `GET /api/properties/{propertyId}/rooms` (`OWNER`, `MANAGER`, `STAFF`) / `POST /api/properties/{propertyId}/rooms` (`OWNER`, `MANAGER`)
- `POST /api/properties/{propertyId}/rooms/{roomId}/status` (`OWNER`, `MANAGER`, `STAFF`) — `{ status: "AVAILABLE" | "MAINTENANCE" | "INACTIVE" }` (`409` if `OCCUPIED`).

## Tenants, Contracts & Handover
- `GET /api/tenants` (`OWNER`, `MANAGER`, `STAFF`) / `POST /api/tenants` (`OWNER`, `MANAGER`)
- `POST /api/tenants/{tenantId}/portal-account` (`OWNER`, `MANAGER`) — `{ password (12..72) }` creates & links a `TENANT` user login.
- `GET /api/contracts` (`OWNER`, `MANAGER`, `STAFF`) / `POST /api/contracts` (`OWNER`, `MANAGER`)
- `POST /api/contracts/{contractId}/activate` (`OWNER`, `MANAGER`) — Transitions `DRAFT` → `ACTIVE`, sets room `OCCUPIED`.
- `POST /api/contracts/{contractId}/terminate` (`OWNER`, `MANAGER`) — Settles deposit + creates `HandoverResponse`.
- `GET /api/contracts/{contractId}/handover` (`OWNER`, `MANAGER`, `STAFF`)

## Utilities, Invoices & Payments
- `GET /api/utility-tariffs` (`OWNER`, `MANAGER`, `STAFF`) / `POST /api/utility-tariffs` (`OWNER`, `MANAGER`)
- `GET /api/meter-readings?billingPeriod=YYYY-MM` (`OWNER`, `MANAGER`, `STAFF`) / `POST /api/meter-readings` (`OWNER`, `MANAGER`, `STAFF`)
- `GET /api/invoices?billingPeriod=YYYY-MM` (`OWNER`, `MANAGER`, `STAFF`)
- `POST /api/invoices/generate` (`OWNER`, `MANAGER`) — `{ billingPeriod }`, auto-applies organization recurring fees.
- `PATCH /api/invoices/{invoiceId}/adjustments` (`OWNER`, `MANAGER`) — `{ recurringFeeAmount, serviceFeeAmount, discountAmount, adjustmentNotes }` (draft only).
- `POST /api/invoices/{invoiceId}/issue` (`OWNER`, `MANAGER`)
- `POST /api/invoices/{invoiceId}/payments` (`OWNER`, `MANAGER`, `STAFF`)

## Maintenance & Notifications
- `GET /api/maintenance-tickets` (`OWNER`, `MANAGER`, `STAFF`) / `POST /api/maintenance-tickets` (`OWNER`, `MANAGER`, `STAFF`)
- `POST /api/maintenance-tickets/{ticketId}/assign` (`OWNER`, `MANAGER`)
- `POST /api/maintenance-tickets/{ticketId}/status` (`OWNER`, `MANAGER`, `STAFF`)
- `GET /api/notifications` (`OWNER`, `MANAGER`, `STAFF`) / `POST /api/notifications/{notificationId}/read` (`OWNER`, `MANAGER`, `STAFF`)

## Tenant Self-Service Portal (`TENANT` only)
- `GET /api/portal/contracts` — Active/historical contracts owned by the authenticated tenant user.
- `GET /api/portal/invoices` — Issued/settled invoices belonging to the authenticated tenant's contracts.
- `GET /api/portal/maintenance-tickets` — Maintenance requests linked to the authenticated tenant's contracts/rooms.
- `POST /api/portal/maintenance-tickets` — `{ roomId, title, description, priority }` submits a new maintenance request for an active contract room owned by the tenant.
