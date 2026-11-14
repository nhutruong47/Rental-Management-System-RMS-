# Final Delivery Report — Rental Management System (RMS)

**Date:** 2026-11-14  
**Repository:** `https://github.com/nhutruong47/Rental-Management-System-RMS-` (`main`)  
**Final Status:** **VERIFIED RUNNABLE**

---

## 1. Project Summary

Rental Management System (RMS) is a Vietnamese-language, organization-isolated web platform for managing rental housing portfolios (`nhà trọ`, `căn hộ dịch vụ`). Built as a Modular Monolith using Java 21 / Spring Boot 3.5.16, PostgreSQL 16 with Flyway migrations (`V1`–`V4`), and a React 19 / TypeScript / Vite SPA behind Nginx, the system supports complete end-to-end operational workflows from property/room setup through contracting, utility metering, VND invoicing, manual payment allocation, maintenance ticketing, in-app notifications, and contract termination with deposit settlement and room release.

---

## 2. Implemented Features

### P0 Core Workflows (`WF-001` – `WF-004`)
- **Organization-Isolated Authentication (`FR-001`, `FR-002`)**: Atomic owner + organization registration, BCrypt(12) password hashing, HttpOnly `RMSSESSION` cookie, `X-XSRF-TOKEN` CSRF protection, `/api/auth/me` session restore across reloads, and session invalidation on logout.
- **Property & Room Management (`FR-003`, `FR-004`, `WF-001`)**: Property directory, unique room codes per property, non-occupied room operational status toggle (`AVAILABLE` ↔ `MAINTENANCE`), and protection blocking manual alteration of `OCCUPIED` rooms.
- **Tenant Directory & Exclusive Contracts (`FR-005`, `FR-006`, `WF-002`)**: Minimal tenant contact records, draft contract creation with `monthlyRent` and `depositAmount` snapshots, and transactional contract activation rejecting overlapping active dates or unavailable rooms.
- **Configurable Utilities & VND Billing (`FR-007`, `FR-008`, `WF-003`)**: Operator-configured electricity/water unit tariffs (`effectiveFrom`), chronological monotonic meter readings, deterministic `BigDecimal` VND integer invoice generation, and immutable issued snapshots.
- **Manual Payment Allocation & Scoped Dashboard (`FR-009`, `FR-010`, `WF-004`)**: Cash and manual bank transfer recording with `idempotencyKey` conflict detection, partial/full invoice balance updates, overpayment rejection, real-time occupancy donut, and per-room period cashflow chart.

### P1 Extended Workflows (`WF-005`, `WF-006`, Notifications)
- **Maintenance Ticketing (`FR-012`, `WF-005`)**: Room-scoped ticket creation (`LOW`–`URGENT`), technician assignment, state-machine-enforced transitions (`OPEN` → `ASSIGNED` → `IN_PROGRESS` / `ON_HOLD` → `RESOLVED` → `CLOSED` / `REOPENED`), and estimated vs actual cost tracking.
- **Contract Termination, Deposit Settlement & Handover (`FR-011`, `FR-013`, `WF-006`)**: Enforces settlement of draft/unpaid contract invoices, calculates `depositRefunded = depositHeld - depositDeducted`, captures final electricity/water readings and checklist notes in `handover_records`, transitions contract to `TERMINATED`, and releases the room back to `AVAILABLE`.
- **In-App Notifications (`Module 10`)**: Database-persisted organization notifications emitted on invoice issue, payment recording, maintenance updates, and contract termination, wired to the topbar bell panel with mark-read support.

---

## 3. Incomplete / Deferred Features

- **Role Invitation & Tenant Self-Service (`FR-014`)**: `MembershipRole` (`OWNER`, `MANAGER`, `STAFF`) exists in schema/domain, while self-service tenant login, staff invitation UI, and email password reset remain deferred.
- **External Integrations & Document Export**: Payment gateways, bank webhooks, legal e-signatures, official Vietnamese e-tax invoices (`hóa đơn điện tử`), PDF export, and automated SMS/email alerts are explicitly out of scope for this release.

---

## 4. Architecture & Database

- **Architecture:** See [ARCHITECTURE.md](ARCHITECTURE.md) and [API.md](API.md). `spring.jpa.open-in-view: false` with all DTO projections inside `@Transactional` service methods.
- **Database:** PostgreSQL 16 owned by Flyway migrations `V1__identity_property_room.sql`, `V2__tenant_contract.sql`, `V3__utility_billing_payments.sql`, and `V4__maintenance_and_handover.sql`. See [ERD.md](ERD.md).

---

## 5. Test Results

| Suite | Result | Details |
|---|---|---|
| Backend Integration Suite (`mvn -B test` in `maven:3.9.9-eclipse-temurin-21`) | **PASS (21/21)** | 0 failures, 0 errors, 0 skipped (`RentalManagementApiIntegrationTest`, including non-transactional `open-in-view: false` full workflow, room status lock, WF-005 maintenance transitions, WF-006 deposit settlement & handover, cross-org denials). |
| Frontend TypeScript Check (`npm --prefix frontend run check`) | **PASS** | `tsc -b --pretty false` exit 0. |
| Frontend UI Suite (`npm --prefix frontend run test -- --reporter=verbose`) | **PASS (3/3)** | `src/App.test.tsx`: contract activation, utility tariffs/readings display, and maintenance ticket assignment. |
| Frontend Production Build (`npm --prefix frontend run build`) | **PASS** | `tsc -b && vite build` exit 0. |
| Docker Compose + PostgreSQL 16 + Flyway | **PASS** | `postgres`, `backend`, and `frontend` all `Up (healthy)`; `flyway_schema_history` verified `V1`–`V4` (`4/4` `success = t`). |
| Live Browser E2E QA (`http://127.0.0.1:55173/`) | **PASS** | Full P0 + P1 operator journeys verified in headless Chromium against PostgreSQL + Spring Boot + Nginx SPA (`CONSOLE_ERRORS: []`, `HTTP_ERRORS: []`). |

---

## 6. Security Findings & Known Issues

- Verified: BCrypt(12), HttpOnly `RMSSESSION`, `X-XSRF-TOKEN` CSRF enforcement (`403` when missing), `DispatcherType.ERROR` permit preserving `400`/`403`/`409` JSON error payloads, and strict organization isolation (`404` / empty lists across tenants).
- Known issues & legal notes: See [ISSUES.md](ISSUES.md) and [KNOWN_LIMITATIONS.md](KNOWN_LIMITATIONS.md) — notably `I-003` (host port overrides `POSTGRES_PORT=55432 FRONTEND_PORT=55173` on this machine) and `I-005` (Vietnamese legal/tax review required before production rollout).
- No secrets or real credentials are committed (`grep` scan clean; `.env` excluded in `.gitignore`).

---

## 7. How to Run & Development Accounts

```bash
# Default ports (5432, 5173):
docker compose up --build -d

# Custom ports if 5432 or 5173 are occupied:
POSTGRES_PORT=55432 FRONTEND_PORT=55173 docker compose up --build -d
```

- Open `http://localhost:5173/` (or `http://localhost:55173/`) and use **"Tạo tài khoản"** on the login screen to provision an isolated development organization and owner account (no hardcoded production credentials are shipped).

---

## 8. Next Steps

1. Review [ASSUMPTIONS.md](ASSUMPTIONS.md) and [ISSUES.md](ISSUES.md) (`I-005`) with Vietnamese legal/accounting counsel before commercial use.
2. When expanding beyond single-owner operation, add `MANAGER`/`STAFF` invitation flows and `TENANT` portal views (`FR-014`).
3. Enable `SESSION_COOKIE_SECURE=true` behind HTTPS termination and add reverse-proxy rate limiting before internet exposure.
