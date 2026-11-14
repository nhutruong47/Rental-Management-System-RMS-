# Project status

Last updated: 2026-11-14

## Current phase

**VERIFIED RUNNABLE (P0 + P1 CORE DELIVERED)** — All P0 slices (S0–S5) and P1 maintenance (`WF-005`), contract termination & deposit settlement handover (`WF-006`), room operational status toggle, and in-app notifications are implemented, integrated, and verified end-to-end across backend, PostgreSQL 16/Flyway, React/TypeScript frontend, Docker Compose, and real browser workflows.

## Verified baseline & delivery

- Remote: `https://github.com/nhutruong47/Rental-Management-System-RMS-`, public, default branch `main`.
- Local clone: `/home/ubuntu/Rental-Management-System-RMS-`, branch `main`. Unrelated workspace repositories (such as `/home/ubuntu/hotel`) were not modified.
- Database schema: Flyway migrations `V1__identity_property_room.sql`, `V2__tenant_contract.sql`, `V3__utility_billing_payments.sql`, and `V4__maintenance_and_handover.sql` applied cleanly on PostgreSQL 16 (`flyway_schema_history` verified 4/4 `success = t`).
- Full-stack Compose verification executed with `POSTGRES_PORT=55432 FRONTEND_PORT=55173` (host `5432` and `5173` occupied by existing host services).

## Priority sequence

| ID | Slice | State | Acceptance evidence |
|---|---|---|---|
| S0 | Repository bootstrap and traceability | DONE | Tracking docs, `.gitignore`, Compose manifest, environment report |
| S1 | Identity / organization isolation + property and room CRUD | DONE | BCrypt auth, `RMSSESSION`, `X-XSRF-TOKEN`, `/api/auth/me`, room status toggles & cross-org isolation tests |
| S2 | Tenant + contract activation / occupancy | DONE | Tenant/contract API, V2 migration, exclusive room activation, overlap rejection, UI activation flow |
| S3 | Configurable meter readings + invoice generation / issue | DONE | VND `BigDecimal`, V3 migration, `GET/POST /api/utility-tariffs`, monotonic readings, invoice snapshot & issue |
| S4 | Manual payment, invoice allocation, balance and dashboard | DONE | Idempotent partial/full manual payment allocation, overpayment rejection, real scoped dashboard & UI |
| S5 | Integrated regression, security review & documentation | DONE | 21/21 backend integration tests, 3/3 Vitest UI tests, TypeScript & Vite build, PostgreSQL/Flyway, full browser E2E QA |
| S6 | P1 Maintenance (`WF-005`), Handover/Deposit Settlement (`WF-006`) & Notifications | DONE | V4 migration, ticket state machine, invoice-settled termination + deposit refund + room release, in-app notification feed |

Role invitation UI, tenant self-service portal (`FR-014`), PDF export, payment gateways, e-signatures, and e-tax invoices remain deferred post-MVP scope. See [FINAL_DELIVERY_REPORT.md](FINAL_DELIVERY_REPORT.md).
