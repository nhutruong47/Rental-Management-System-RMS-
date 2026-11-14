# Architecture decisions

## D-001 — Build from an empty repository (Accepted)

GitHub reported repository size 0, `git ls-remote` had no refs, and a fresh clone contained only `.git`. No legacy RMS code, tests or status documents exist. Start the RMS as a new project; do not copy unrelated Nhu Villas or other workspace code.

## D-002 — Modular monolith (Accepted)

Use a Spring Boot REST backend and a React/TypeScript SPA backed by PostgreSQL/Flyway. Keep domain packages organized by workflow/module inside one deployable backend; avoid distributed services, message brokers and Redis in MVP. This matches the requested stack and keeps local end-to-end verification feasible.

## D-003 — Organization membership is the security boundary (Accepted)

Every business record is associated directly or transitively with one organization. Resolve organization identity only from the authenticated server-side principal, never a client-supplied organization ID. Add repository predicates and tests for cross-organization access. MVP owner registration provisions one organization and owner account; staff invitation is later scope.

## D-004 — Same-origin session authentication (Accepted)

Prefer server-managed HttpOnly session cookies with CSRF protection. Frontend dev proxy and Compose reverse proxy should route `/api` to backend, avoiding permissive CORS and storing bearer secrets in browser storage. Enforce a non-default owner password, BCrypt, session fixation protection and logout invalidation. Production cookie Secure behavior is configuration-dependent and must be explicitly enabled behind TLS.

## D-005 — PostgreSQL and Flyway own schema (Accepted)

Use PostgreSQL for local integration and versioned SQL migrations. Hibernate schema generation is validation only; no `create`/`update`. Use constraints/indexes for tenant isolation, unique organization-scoped room codes and invoice periods, nonnegative monetary/range checks, and immutable issued invoice snapshots.

## D-006 — Vietnam business values (Accepted)

Use VND with `BigDecimal` and explicit integer-VND rounding (HALF_UP); use `Asia/Ho_Chi_Minh` for business display and local billing dates, with UTC instants for event timestamps. Configure utility unit prices per organization/property; do not encode an official tariff or claim tax/legal compliance.

## D-007 — One-room exclusive rental in MVP (Accepted)

A contract occupies one room for one tenant. Activation is transactional, locks the room, checks organization scope, room state and date-range overlap against all occupying contracts, and snapshots rent/deposit amounts. Co-occupants, reservations, mid-period prorating, meter rollover, shared meters and legal electronic signatures remain out of scope until specified and tested.

## D-008 — Defer non-P0 slices (Accepted)

Focus on owner sign-up/isolation, properties/rooms, tenants/contracts, configurable readings and deterministic invoices, manual payments and dashboard. Do not dilute reliability with gateway, notifications, PDF, SaaS billing, or maintenance before core flows pass integrated tests.

## D-009 — Containerized Java build (Accepted)

Java 21 was verified in `eclipse-temurin:21-jdk`; host Java and Maven are absent. Build and test with a pinned Maven/Temurin container instead of installing system packages. Use local Docker/PostgreSQL where available and check limited disk headroom.
