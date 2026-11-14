# Issues and blockers

Updated: 2026-11-14

| ID | Priority | State | Issue / risk | Evidence / impact | Resolution / next action |
|---|---|---|---|---|---|
| I-001 | HIGH | RESOLVED | Repository was empty at baseline. | Initial clone contained only `.git`. | Full backend, migrations, React/TypeScript SPA, Compose stack, and docs implemented and verified. |
| I-002 | MEDIUM | RESOLVED | Java and Maven are absent from host. | Host lacks `java`/`mvn`; `maven:3.9.9-eclipse-temurin-21` container executes builds and tests reliably. | Documented containerized Maven run path in `README.md`. |
| I-003 | MEDIUM | OPEN (ENV) | Host ports `5432` and `5173` occupied on test machine. | Existing host listeners bound to `5432` and `5173`. | `docker-compose.yml` supports `${POSTGRES_PORT:-5432}` and `${FRONTEND_PORT:-5173}`; verified with `POSTGRES_PORT=55432 FRONTEND_PORT=55173`. |
| I-004 | HIGH | RESOLVED | Integration/authorization/OSIV coverage gaps caught during QA. | Initial browser QA uncovered CSRF header mismatch (`X-CSRF-TOKEN` vs `X-XSRF-TOKEN`), `/error` forwarding to 401, missing `GET /api/utility-tariffs`, and `LazyInitializationException` under `open-in-view: false`. | Fixed all four root causes, added non-transactional integration test (`fullWorkflowSucceedsWithoutOuterTransactionWhenOpenInViewIsDisabled`), and re-verified E2E in browser. |
| I-005 | HIGH | OPEN (LEGAL) | Vietnamese legal/accounting questions unverified. | Contract validity, privacy/data handling, residence reporting, utility prices, tax/e-invoice status are not legally certified. | Obtain qualified Vietnamese legal/accounting review before production rollout; UI clearly labels internal statements and configurable rates. |
| I-006 | LOW | OPEN (ENV) | `gh` CLI unavailable on host. | `gh: command not found`; standard `git` over SSH remote works. | Use `git` directly for commits and pushes. |
