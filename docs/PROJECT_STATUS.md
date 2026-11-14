# Project status

Last updated: 2026-11-14

## Current phase

**MVP CORE COMPLETE & VERIFIED** — All P0 slices (S0–S5) are implemented, integrated, and verified end-to-end across backend, PostgreSQL/Flyway, React/TypeScript frontend, Docker Compose, and real browser workflows.

## Verified baseline & delivery

- Remote: `https://github.com/nhutruong47/Rental-Management-System-RMS-`, public, default branch `main`.
- Local clone: `/home/ubuntu/Rental-Management-System-RMS-`, branch `main`. Unrelated workspace repositories (such as `/home/ubuntu/hotel`) were not modified.
- Database schema: Flyway migrations `V1__identity_property_room.sql`, `V2__tenant_contract.sql`, and `V3__utility_billing_payments.sql` applied cleanly on PostgreSQL 16 (`flyway_schema_history` verified 3/3 `success = t`).
- Full-stack Compose verification executed with `POSTGRES_PORT=55432 FRONTEND_PORT=55173` (host `5432` and `5173` occupied by existing host services).

## MVP Definition of Done

A small Vietnamese rental operator can register/sign in as an organization owner; create a property and room; add a tenant and create/activate an exclusive-room contract; configure utility tariffs, record monotonic meter readings, and generate/issue a deterministically calculated VND invoice; record and allocate manual payments; and view scoped occupancy and financial metrics on the owner dashboard. Organization isolation, CSRF/session security, database constraints, validation, error responses, frontend integration, automated tests, and real browser E2E verification are complete. External payment gateways, electronic signatures, tax e-invoice compliance, SaaS subscriptions, and public deployment are explicitly out of MVP scope.

## Priority sequence

| ID | Slice | State | Acceptance evidence |
|---|---|---|---|
| S0 | Repository bootstrap and traceability | DONE | Tracking docs, `.gitignore`, Compose manifest, environment report |
| S1 | Identity / organization isolation + property and room CRUD | DONE | BCrypt auth, `RMSSESSION`, `X-XSRF-TOKEN`, `/api/auth/me`, property/room API & cross-org isolation tests |
| S2 | Tenant + contract activation / occupancy | DONE | Tenant/contract API, V2 migration, exclusive room activation, overlap rejection, UI activation flow |
| S3 | Configurable meter readings + invoice generation / issue | DONE | VND `BigDecimal`, V3 migration, `GET/POST /api/utility-tariffs`, monotonic readings, invoice snapshot & issue |
| S4 | Manual payment, invoice allocation, balance and dashboard | DONE | Idempotent partial/full manual payment allocation, overpayment rejection, real scoped dashboard & UI |
| S5 | Integrated regression, security review, documentation and final report | DONE | 18/18 backend integration tests, 2/2 Vitest UI tests, TypeScript & Vite build, PostgreSQL/Flyway, full browser E2E QA |

Maintenance, notification delivery, PDF export, deposit settlement, and contract termination/handover (WF-005/WF-006, FR-011–FR-014) remain explicitly deferred as P1 post-MVP work.
