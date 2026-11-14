# Requirements traceability matrix

Updated: 2026-11-14. Status reflects implemented and verified code/tests.

## MVP & P1 functional requirements

| ID | Requirement | Actor | Priority | Acceptance criteria (summary) | API/UI | Test evidence | State |
|---|---|---|---|---|---|---|---|
| FR-001 | Register an owner and establish an isolated organization. | Owner | P0 | Unique account; organization membership created atomically; BCrypt hashed; server-derived org scope. | `POST /api/auth/register`, Auth UI | Auth integration tests + browser QA | DONE |
| FR-002 | Authenticate, logout, manage session and profile. | User | P0 | Invalid credentials denied; `RMSSESSION` invalidated on logout; `X-XSRF-TOKEN` CSRF + `/api/auth/me` session restore. | Auth UI/API (`/login`, `/logout`, `/me`, `/csrf`) | Auth/CSRF/logout integration tests + browser QA | DONE |
| FR-003 | Create and view organization properties. | Owner | P0 | Required fields validated; resources scoped to principal organization. | `GET/POST /api/properties`, Properties UI | CRUD/validation/isolation tests + browser QA | DONE |
| FR-004 | Manage rooms and availability states. | Owner | P0 | Unique property room code; nonnegative rent/deposit; `AVAILABLE`/`OCCUPIED`/`MAINTENANCE` transitions; org-scoped access. | `GET/POST /api/properties/{id}/rooms`, `POST .../status`, Properties UI | Duplicate/status-lock/isolation tests + browser QA | DONE |
| FR-005 | Create and manage tenant profiles with minimum necessary personal data. | Owner | P0 | Validated contact fields; sensitive ID-document collection deferred; org-scoped list. | `GET/POST /api/tenants`, Tenants UI | Isolation tests + browser QA | DONE |
| FR-006 | Create and activate a room rental contract. | Owner | P0 | Rent & deposit snapshots; valid dates; activation validates room availability & overlap; org-scoped. | `GET/POST /api/contracts`, `POST /api/contracts/{id}/activate`, Contracts UI | Activation/overlap/non-OSIV tests + browser QA | DONE |
| FR-007 | Record utility meter readings and configurable per-unit tariffs. | Owner | P0 | Monotonic readings; chronological period enforcement; operator-configured per-unit tariffs. | `GET/POST /api/utility-tariffs`, `GET/POST /api/meter-readings`, Utilities UI | Order/duplicate/validation tests + browser QA | DONE |
| FR-008 | Generate and issue monthly VND invoice. | Owner | P0 | Deterministic rent/utility snapshot; integer đồng `BigDecimal` rounding; duplicate period skipped; issue transition. | `GET /api/invoices`, `POST /api/invoices/generate`, `POST /api/invoices/{id}/issue`, Invoices UI | Totals/idempotency/isolation tests + browser QA | DONE |
| FR-009 | Record a manual cash/bank payment and allocate to an invoice. | Owner | P0 | Positive amount; actor recorded; idempotency key guarded; partial/full payments update balance atomically; overpayment rejected. | `POST /api/invoices/{id}/payments`, Invoices UI | Partial/idempotency/overpayment/isolation tests + browser QA | DONE |
| FR-010 | View basic owner occupancy and financial dashboard. | Owner | P0 | Scoped occupancy counts, billed vs collected vs outstanding metrics, per-room period chart derived from persisted data. | Dashboard UI + `/api/notifications` | Vitest UI tests + browser QA | DONE |
| FR-011 | Record deposit receipt/settlement. | Owner | P1 | Deposit snapshotted on contract and settled (`deducted + refunded = held`) in handover record, distinct from rent revenue. | `ContractService.terminate`, `HandoverRecord`, Contracts UI | Termination & deposit arithmetic tests + browser QA | DONE |
| FR-012 | Create, assign and close maintenance request. | Owner | P1 | Valid state transitions (`OPEN`..`CLOSED`/`REOPENED`), cost tracking, and room/organization scope enforced. | `GET/POST /api/maintenance-tickets`, `.../assign`, `.../status`, Maintenance UI | State machine & cross-org isolation tests + Vitest + browser QA | DONE |
| FR-013 | Terminate contract, settle final bill/deposit, complete handover, release room. | Owner | P1 | Blocks unpaid invoices, captures final readings & checklist, retains history, releases room to `AVAILABLE`. | `POST /api/contracts/{id}/terminate`, `GET .../handover`, Contracts UI | Termination/handover integration tests + browser QA | DONE |
| FR-014 | Provide tenant self-service portal. | Tenant | P1 | Tenant sees only own contract, invoices, payments and requests. | Deferred | — | DEFERRED |

## Non-functional requirements

| ID | Requirement | Acceptance / verification | State |
|---|---|---|---|
| NFR-001 | Organization isolation | Backend derives `organizationId` from authenticated `RmsUserPrincipal`; cross-org reads/writes tested. | DONE |
| NFR-002 | Data consistency | Flyway V1–V4 constraints, foreign/unique/check constraints and transactions verified on PostgreSQL 16. | DONE |
| NFR-003 | Security | BCrypt(12), `RMSSESSION` HttpOnly cookie, `X-XSRF-TOKEN` CSRF, `DispatcherType.ERROR` permit, safe JSON error responses. | DONE |
| NFR-004 | Money correctness | `BigDecimal` arithmetic, `RoundingMode.HALF_UP` integer VND, immutable invoice snapshots verified. | DONE |
| NFR-005 | Locale/time | Vietnamese UI copy, `vi-VN` currency/date formatting, UTC timestamps (`created_at`, `issued_at`, `recorded_at`). | DONE |
| NFR-006 | Usability | Responsive layout, loading/empty/error/success states, no decorative/fake metrics or non-functional actions. | DONE |
| NFR-007 | Reproducibility | Documented `docker compose up --build -d`, pinned images, backend/frontend builds and migrations verified. | DONE |
