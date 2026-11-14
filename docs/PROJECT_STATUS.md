# Project status

Last updated: 2026-11-14

## Current phase

**VERIFIED RUNNABLE (FULL V1–V5 DELIVERED)** — All P0 slices (S0–S5), P1 operations (`S6`: maintenance `WF-005`, contract termination & deposit settlement handover `WF-006`, room operational status toggle, in-app notifications), and V5 enterprise capabilities (`S7`: 4-role RBAC `OWNER`/`MANAGER`/`STAFF`/`TENANT`, Tenant Self-Service Portal `FR-014`, Organization Settings recurring fees, draft invoice adjustments, login rate limiter, self-service password change, and immutable audit logs) are implemented, integrated, and verified end-to-end across backend, PostgreSQL 16/Flyway, React/TypeScript frontend, Docker Compose, and real browser workflows.

## Verified baseline & delivery

- Remote: `https://github.com/nhutruong47/Rental-Management-System-RMS-`, public, default branch `main`.
- Local clone: `/home/ubuntu/Rental-Management-System-RMS-`, branch `main`. Unrelated workspace repositories (such as `/home/ubuntu/hotel`) were not modified.
- Database schema: Flyway migrations `V1__identity_property_room.sql`, `V2__tenant_contract.sql`, `V3__utility_billing_payments.sql`, `V4__maintenance_and_handover.sql`, and `V5__rbac_audit_settings_recurring.sql` applied cleanly on PostgreSQL 16 (`flyway_schema_history` verified 5/5 `success = t`).
- Full-stack Compose verification executed with `POSTGRES_PORT=55432 FRONTEND_PORT=55173` (host `5432` and `5173` occupied by existing host services).

## Priority sequence

| ID | Slice | State | Acceptance evidence |
|---|---|---|---|
| S0 | Repository bootstrap and traceability | DONE | Tracking docs, `.gitignore`, Compose manifest, environment report |
| S1 | Identity / organization isolation + property and room CRUD | DONE | Spring Security sessions, `RMSSESSION`, `X-XSRF-TOKEN`, `/api/auth/me`, room status toggles & cross-org isolation tests |
| S2 | Tenant + contract activation / occupancy | DONE | Tenant/contract API, V2 migration, exclusive room activation, overlap rejection, UI activation flow |
| S3 | Configurable meter readings + invoice generation / issue | DONE | VND `BigDecimal`, V3 migration, `GET/POST /api/utility-tariffs`, monotonic readings, invoice snapshot & issue |
| S4 | Manual payment, invoice allocation, balance and dashboard | DONE | Idempotent partial/full manual payment allocation, overpayment rejection, real scoped dashboard & UI |
| S5 | Integrated regression, security review & documentation | DONE | Backend integration tests, Vitest UI tests, TypeScript & Vite build, PostgreSQL/Flyway, browser E2E QA |
| S6 | P1 Maintenance (`WF-005`), Handover/Deposit Settlement (`WF-006`) & Notifications | DONE | V4 migration, ticket state machine, invoice-settled termination + deposit refund + room release, in-app notification feed |
| S7 | V5 RBAC, Tenant Portal (`FR-014`), Settings, Adjustments, Rate Limiter & Audit Logs | DONE | V5 migration, `@PreAuthorize` across all 12 controllers, `TenantPortalController` + `CỔNG NGƯỜI THUÊ` UI, `organization_settings`, `audit_logs`, 24/24 backend tests, 7/7 Vitest tests, browser E2E across OWNER/STAFF/TENANT |

## Deferred post-v1 items

PDF invoice export, automated bank/payment-gateway reconciliation, automated email/SMS reminders, and e-tax/e-signature integrations remain outside scope. See [FINAL_DELIVERY_REPORT.md](FINAL_DELIVERY_REPORT.md) and [KNOWN_LIMITATIONS.md](KNOWN_LIMITATIONS.md).
