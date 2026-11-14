# Requirements traceability matrix

Updated: 2026-11-14. Status reflects implemented and verified code/tests (`V1`–`V5`).

## Functional requirements

| ID | Requirement | Actor | Priority | Acceptance criteria (summary) | API/UI | Test evidence | State |
|---|---|---|---|---|---|---|---|
| FR-001 | Register an owner and establish an isolated organization. | Owner | P0 | Unique account; organization membership created atomically; BCrypt hashed; server-derived org scope. | `POST /api/auth/register`, Auth UI | Auth integration tests + browser QA | DONE |
| FR-002 | Authenticate, logout, manage session, rate limiting, and password change. | User | P0 | Invalid credentials denied (`429` after 10 failures / 60s); `RMSSESSION` invalidated on logout; `X-XSRF-TOKEN` CSRF + `/api/auth/me` restore; `/api/auth/change-password`. | Auth UI/API (`/login`, `/logout`, `/me`, `/csrf`, `/change-password`) | Auth/CSRF/rate-limit/change-password tests + browser QA | DONE |
| FR-003 | Create and view organization properties. | Owner / Manager / Staff | P0 | Required fields validated; resources scoped to principal organization; RBAC enforced. | `GET/POST /api/properties`, Properties UI | CRUD/validation/isolation tests + browser QA | DONE |
| FR-004 | Manage rooms and availability states. | Owner / Manager / Staff | P0 | Unique property room code; nonnegative rent/deposit; `AVAILABLE`/`OCCUPIED`/`MAINTENANCE` transitions; org-scoped access. | `GET/POST /api/properties/{id}/rooms`, `POST .../status`, Properties UI | Duplicate/status-lock/isolation tests + browser QA | DONE |
| FR-005 | Create and manage tenant profiles with minimum necessary personal data & portal provisioning. | Owner / Manager | P0 | Validated contact fields; sensitive ID-document collection deferred; optional `TENANT` portal account provisioning (`userId` link). | `GET/POST /api/tenants`, `POST .../portal-account`, Tenants UI | Isolation & portal provisioning tests + browser QA | DONE |
| FR-006 | Create and activate a room rental contract. | Owner / Manager | P0 | Rent & deposit snapshots; valid dates; activation validates room availability & overlap; org-scoped. | `GET/POST /api/contracts`, `POST /api/contracts/{id}/activate`, Contracts UI | Activation/overlap/non-OSIV tests + browser QA | DONE |
| FR-007 | Record utility meter readings and configurable per-unit tariffs. | Owner / Manager / Staff | P0 | Monotonic readings; chronological period enforcement; operator-configured per-unit tariffs. | `GET/POST /api/utility-tariffs`, `GET/POST /api/meter-readings`, Utilities UI | Order/duplicate/validation tests + browser QA | DONE |
| FR-008 | Generate, adjust draft, and issue monthly VND invoice. | Owner / Manager | P0 | Deterministic rent/utility/recurring-fee snapshot; draft service/discount adjustments (`ck_invoice_amounts`); integer đồng `BigDecimal`; issue transition. | `GET /api/invoices`, `POST /api/invoices/generate`, `PATCH /api/invoices/{id}/adjustments`, `POST .../issue`, Invoices UI | Totals/recurring/adjustment/isolation tests + browser QA | DONE |
| FR-009 | Record a manual cash/bank payment and allocate to an invoice. | Owner / Manager / Staff | P0 | Positive amount; actor recorded; idempotency key guarded; partial/full payments update balance atomically; overpayment rejected. | `POST /api/invoices/{id}/payments`, Invoices UI | Partial/idempotency/overpayment/isolation tests + browser QA | DONE |
| FR-010 | View owner/operator occupancy, financial dashboard, settings & audit logs. | Owner / Manager / Staff | P0 | Scoped occupancy counts, billed vs collected vs outstanding metrics, organization settings (`organization_settings`), immutable `audit_logs`. | Dashboard, Settings & Audit UI + `/api/settings`, `/api/audit-logs`, `/api/members` | Vitest UI tests + integration tests + browser QA | DONE |
| FR-011 | Record deposit receipt/settlement. | Owner / Manager | P1 | Deposit snapshotted on contract and settled (`deducted + refunded = held`) in handover record, distinct from rent revenue. | `ContractService.terminate`, `HandoverRecord`, Contracts UI | Termination & deposit arithmetic tests + browser QA | DONE |
| FR-012 | Create, assign and close maintenance request. | Owner / Manager / Staff / Tenant | P1 | Valid state transitions (`OPEN`..`CLOSED`/`REOPENED`), cost tracking, and room/organization scope enforced. | `GET/POST /api/maintenance-tickets`, `.../assign`, `.../status`, Maintenance UI | State machine & cross-org isolation tests + Vitest + browser QA | DONE |
| FR-013 | Terminate contract, settle final bill/deposit, complete handover, release room. | Owner / Manager | P1 | Blocks unpaid invoices, captures final readings & checklist, retains history, releases room to `AVAILABLE`. | `POST /api/contracts/{id}/terminate`, `GET .../handover`, Contracts UI | Termination/handover integration tests + browser QA | DONE |
| FR-014 | Provide tenant self-service portal. | Tenant | P1 | Tenant sees only own contracts, invoices, payments and requests (`tenants.user_id` binding); can submit maintenance tickets and change password. | `GET /api/portal/{contracts,invoices,maintenance-tickets}`, `POST /api/portal/maintenance-tickets`, `CỔNG NGƯỜI THUÊ` UI | Tenant portal IDOR integration tests + Vitest + browser QA | DONE |

## Non-functional requirements

| ID | Requirement | Acceptance / verification | State |
|---|---|---|---|
| NFR-001 | Organization & Tenant Isolation | Backend derives `organizationId` & `userId` from authenticated `RmsUserPrincipal`; cross-org & cross-tenant IDOR reads/writes tested. | DONE |
| NFR-002 | Data Consistency | Flyway V1–V5 constraints, composite tenant FKs, unique/check constraints and transactions verified on PostgreSQL 16. | DONE |
| NFR-003 | Security | BCrypt(12), `RMSSESSION` HttpOnly cookie, `X-XSRF-TOKEN` CSRF, `@PreAuthorize` RBAC, `AuthRateLimiter`, `DispatcherType.ERROR` permit, safe JSON error responses, immutable audit log. | DONE |
| NFR-004 | Money Correctness | `BigDecimal` arithmetic, `RoundingMode.HALF_UP` integer VND, `ck_invoice_amounts` constraint, immutable issued invoice snapshots verified. | DONE |
| NFR-005 | Locale/Time | Vietnamese UI copy, `vi-VN` currency/date formatting, UTC timestamps (`created_at`, `issued_at`, `recorded_at`). | DONE |
| NFR-006 | Usability | Responsive layout, loading/empty/error/success states, role-aware navigation, dedicated tenant portal, no decorative/fake metrics. | DONE |
| NFR-007 | Reproducibility | Documented `docker compose up --build -d`, pinned images, backend/frontend builds and Flyway `V1`–`V5` migrations verified. | DONE |
