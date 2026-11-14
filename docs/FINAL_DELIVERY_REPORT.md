# Final Delivery Report — Rental Management System (RMS)

**Date:** 2026-11-14  
**Repository:** `https://github.com/nhutruong47/Rental-Management-System-RMS-` (`main`)  
**Final Status:** **VERIFIED RUNNABLE (FULL V1–V5 RELEASE)**

---

## 1. Project Summary

Rental Management System (RMS) is a Vietnamese-language, organization-isolated web platform for managing rental housing portfolios (`nhà trọ`, `căn hộ dịch vụ`). Built as a Modular Monolith using Java 21 / Spring Boot 3.5.16, PostgreSQL 16 with Flyway migrations (`V1`–`V5`), and a React 19 / TypeScript / Vite SPA behind Nginx, the system delivers end-to-end operator workflows (`OWNER` / `MANAGER` / `STAFF`) and a dedicated self-service **Cổng người thuê** (`TENANT` portal).

---

## 2. Implemented Features

### P0 Core Workflows (`WF-001` – `WF-004`)
- **Organization-Isolated Authentication (`FR-001`, `FR-002`)**: Atomic owner + organization registration, BCrypt(12) password hashing, HttpOnly `RMSSESSION` cookie, `X-XSRF-TOKEN` CSRF protection, `/api/auth/me` session restore across reloads, `/api/auth/change-password`, `AuthRateLimiter`, and session invalidation on logout.
- **Property & Room Management (`FR-003`, `FR-004`, `WF-001`)**: Property directory, unique room codes per property, non-occupied room operational status toggle (`AVAILABLE` ↔ `MAINTENANCE`), and protection blocking manual alteration of `OCCUPIED` rooms.
- **Tenant Directory & Exclusive Contracts (`FR-005`, `FR-006`, `WF-002`)**: Minimal tenant contact records, draft contract creation with `monthlyRent` and `depositAmount` snapshots, and transactional contract activation rejecting overlapping active dates or unavailable rooms.
- **Configurable Utilities & VND Billing (`FR-007`, `FR-008`, `WF-003`)**: Operator-configured electricity/water unit tariffs (`effectiveFrom`), chronological monotonic meter readings, deterministic `BigDecimal` VND integer invoice generation (auto-including organization recurring fees), draft invoice service/discount adjustments, and immutable issued snapshots.
- **Manual Payment Allocation & Scoped Dashboard (`FR-009`, `FR-010`, `WF-004`)**: Cash and manual bank transfer recording with `idempotencyKey` conflict detection, partial/full invoice balance updates, overpayment rejection, real-time occupancy donut, and per-room period cashflow chart.

### P1 Extended Workflows (`WF-005`, `WF-006`, Notifications)
- **Maintenance Ticketing (`FR-012`, `WF-005`)**: Room-scoped ticket creation (`LOW`–`URGENT`), technician assignment, state-machine-enforced transitions (`OPEN` → `ASSIGNED` → `IN_PROGRESS` / `ON_HOLD` → `RESOLVED` → `CLOSED` / `REOPENED`), and estimated vs actual cost tracking.
- **Contract Termination, Deposit Settlement & Handover (`FR-011`, `FR-013`, `WF-006`)**: Enforces settlement of draft/unpaid contract invoices, calculates `depositRefunded = depositHeld - depositDeducted`, captures final electricity/water readings and checklist notes in `handover_records`, transitions contract to `TERMINATED`, and releases the room back to `AVAILABLE`.
- **In-App Notifications (`Module 10`)**: Database-persisted organization notifications emitted on invoice issue, payment recording, maintenance updates, and contract termination, wired to the topbar bell panel with mark-read support.

### V5 Enterprise RBAC, Tenant Portal, Settings & Audit Trail
- **4-Role RBAC (`OWNER`, `MANAGER`, `STAFF`, `TENANT`)**: `@EnableMethodSecurity` + `@PreAuthorize` across all 12 REST controllers, member invitation & role management (`MemberController`), and role-aware SPA navigation hiding restricted views.
- **Tenant Self-Service Portal (`FR-014`)**: `TenantPortalController` (`/api/portal/**`) strictly scoped to `tenants.user_id = principal.userId`, allowing owners/managers to provision tenant logins and allowing tenants to view only their own contracts, invoices, and maintenance requests, submit new tickets, and change their own password.
- **Organization Settings & Recurring Fees (`SettingsController`)**: Configurable default billing due day and monthly recurring internet, garbage, and parking fees (`organization_settings`), automatically summed into `recurring_fee_amount` when generating draft invoices.
- **Immutable Audit Logs (`AuditController`)**: Persists authentication, RBAC, settings, provisioning, and billing adjustments in `audit_logs` with a read-only `OWNER`/`MANAGER` audit log view.

---

## 3. Out-of-Scope / External Deferred Items

- **External Integrations & Document Export**: Payment gateways, bank webhooks, legal e-signatures, official Vietnamese e-tax invoices (`hóa đơn điện tử`), PDF export, and automated SMTP/SMS dispatch remain outside scope for this local/self-hosted release.

---

## 4. Architecture & Database

- **Architecture:** See [ARCHITECTURE.md](ARCHITECTURE.md) and [API.md](API.md). `spring.jpa.open-in-view: false` with all DTO projections inside `@Transactional` service methods.
- **Database:** PostgreSQL 16 owned by Flyway migrations `V1__identity_property_room.sql` through `V5__rbac_audit_settings_recurring.sql`. See [ERD.md](ERD.md).

---

## 5. Test Results

| Suite | Result | Details |
|---|---|---|
| Backend Integration Suite (`mvn -B test` in `maven:3.9.9-eclipse-temurin-21`) | **PASS (24/24)** | 0 failures, 0 errors, 0 skipped (`RentalManagementApiIntegrationTest`, covering P0, P1, V5 RBAC, Tenant Portal IDOR isolation, recurring fees + draft invoice adjustment, rate limiting, password change, and audit logs). |
| Frontend TypeScript Check (`npm --prefix frontend run check`) | **PASS** | `tsc -b --pretty false` exit 0. |
| Frontend UI Suite (`npm --prefix frontend run test -- --reporter=verbose`) | **PASS (7/7)** | `src/App.test.tsx`: contract activation, utility tariffs/readings, maintenance assignment, `STAFF` RBAC nav isolation, `OWNER` members & audit logs, and `TENANT` portal ticket submission. |
| Frontend Production Build (`npm --prefix frontend run build`) | **PASS** | `tsc -b && vite build` exit 0. |
| Docker Compose + PostgreSQL 16 + Flyway | **PASS** | `postgres`, `backend`, and `frontend` all `Up (healthy)`; `flyway_schema_history` verified `V1`–`V5` (`5/5` `success = t`). |
| Live Browser E2E QA (`http://127.0.0.1:55173/`) | **PASS** | Full `OWNER` -> `STAFF` -> `TENANT` -> `OWNER` end-to-end journeys verified in headless Chromium against live PostgreSQL 16 + Spring Boot + Nginx SPA (0 console errors, 0 page errors, 0 backend error/warn logs). |

---

## 6. Security Findings & Known Issues

- Verified: BCrypt(12), HttpOnly `RMSSESSION`, `X-XSRF-TOKEN` CSRF enforcement (`403` when missing), `@PreAuthorize` RBAC enforcement, `AuthRateLimiter`, `DispatcherType.ERROR` permit preserving `400`/`403`/`409`/`429` JSON error payloads, strict organization isolation (`404` / empty lists across tenants), and `TenantPortalController` principal binding.
- Known issues & legal notes: See [ISSUES.md](ISSUES.md) and [KNOWN_LIMITATIONS.md](KNOWN_LIMITATIONS.md) — notably `I-003` (host port overrides `POSTGRES_PORT=55432 FRONTEND_PORT=55173` on this machine) and `I-005` (Vietnamese legal/tax review required before production rollout).
- No secrets or real credentials are committed (`grep` scan clean; `.env` excluded in `.gitignore`).

---

## 7. How to Run & Development Accounts

```bash
# Default ports (5432, 5173):
docker compose up --build -d

# Custom ports if 5432 or 5173 are occupied:
POSTGRES_PORT=55432 FRONTEND_PORT=55173 docker compose up --build -d --wait
```

- Open `http://localhost:5173/` (or `http://localhost:55173/`) and use **"Tạo tài khoản"** on the login screen to provision an isolated development organization and `OWNER` account, then manage `MANAGER`/`STAFF` members or provision `TENANT` portal accounts from the UI.

---

## 8. Next Steps

1. Review [ASSUMPTIONS.md](ASSUMPTIONS.md) and [ISSUES.md](ISSUES.md) (`I-005`) with Vietnamese legal/accounting counsel before commercial use.
2. Enable `SESSION_COOKIE_SECURE=true` behind TLS termination and front with a reverse-proxy/Redis rate limiter if scaling horizontally beyond a single backend container.
