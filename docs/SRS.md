# Software Requirements Specification (SRS) — RMS

## 1. System Context & Locale

- **Language:** Vietnamese (`vi-VN`) user interface, VND integer currency formatting, `Asia/Ho_Chi_Minh` local business dates (`LocalDate`, `YearMonth` `yyyy-MM`), UTC instants (`Instant` / `TIMESTAMPTZ`) for audit/event timestamps.
- **Architecture:** Modular Monolith — Spring Boot 3.5.16 (Java 21) REST API + React 19 / TypeScript / Vite SPA behind Nginx same-origin reverse proxy + PostgreSQL 16 + Flyway (`V1`–`V4`).

## 2. Functional Modules Implemented

1. **Identity & Authentication (`vn.rms.identity`)**: Owner registration (`POST /api/auth/register`), session login (`POST /api/auth/login`), session check (`GET /api/auth/me`), logout invalidation (`POST /api/auth/logout`), CSRF bootstrap (`GET /api/auth/csrf`).
2. **Properties & Rooms (`vn.rms.property`)**: Property CRUD, room creation with organization/property unique `roomCode`, manual status transition (`AVAILABLE` ↔ `MAINTENANCE` / `INACTIVE`, blocked if `OCCUPIED`).
3. **Tenants (`vn.rms.tenant`)**: Minimal personal contact records (`fullName`, `phone`, `email`) without sensitive ID document storage.
4. **Contracts & Handover (`vn.rms.contract`)**: Draft contract creation (rent & deposit snapshots), transactional exclusive activation, termination with invoice settlement check, deposit deduction/refund calculation, final meter readings, handover record persistence, and room release (`OCCUPIED` → `AVAILABLE`).
5. **Utilities (`vn.rms.utility`)**: Configurable per-unit electricity/water tariffs with effective dates (`GET/POST /api/utility-tariffs`), monotonic chronological meter readings (`GET/POST /api/meter-readings`).
6. **Billing & Manual Payments (`vn.rms.billing`)**: Idempotent monthly invoice generation (`POST /api/invoices/generate`), draft → issued immutable snapshot (`POST /api/invoices/{id}/issue`), idempotent partial/full manual payment allocation (`POST /api/invoices/{id}/payments`).
7. **Maintenance (`vn.rms.maintenance`)**: Ticket creation, staff assignment, validated status transitions (`OPEN`, `ASSIGNED`, `IN_PROGRESS`, `ON_HOLD`, `RESOLVED`, `CLOSED`, `REOPENED`), estimated & actual cost tracking.
8. **Notifications (`vn.rms.notification`)**: Org-scoped in-app activity feed emitted on invoice issue, payment recording, maintenance updates, and contract termination.

## 3. Non-Functional Guarantees

- `spring.jpa.open-in-view: false` across all production endpoints (all DTOs materialized inside `@Transactional` boundaries).
- Database constraints enforce non-negative money, monotonic readings, unique `(organization_id, idempotency_key)` payments, unique `(organization_id, contract_id, billing_period)` invoices, and deposit settlement arithmetic (`deposit_deducted + deposit_refunded = deposit_held`).
