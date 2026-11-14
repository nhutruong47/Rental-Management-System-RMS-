# Test results

Updated: 2026-11-14

## Verified test & build runs

| Check | Result | Evidence / notes |
|---|---|---|
| Backend integration suite (`mvn -B test` via `maven:3.9.9-eclipse-temurin-21`) | PASS (24/24) | `RentalManagementApiIntegrationTest`: 24 tests run, 0 failures, 0 errors, 0 skipped. Includes auth/CSRF/session, `/api/auth/me`, `/actuator/health`, cross-org isolation, property/room uniqueness & status lock (`409` when `OCCUPIED`), contract activation & overlap rejection, tariff list/create, monotonic meter reading order, invoice generation/issue/snapshot, idempotent partial payment & overpayment guard, 403 missing CSRF, 400 validation JSON, full non-transactional workflow (`open-in-view: false`), `WF-006` termination/deposit settlement/handover, `WF-005` maintenance state transitions, org-scoped notifications, V5 RBAC (`OWNER`/`MANAGER`/`STAFF`/`TENANT`) + tenant portal IDOR isolation, org settings recurring fees + draft invoice adjustment + audit logs, and login rate limiting + password change. |
| Frontend TypeScript check (`npm --prefix frontend run check`) | PASS | `tsc -b --pretty false` exited 0 with no diagnostics. |
| Frontend UI tests (`npm --prefix frontend run test -- --reporter=verbose`) | PASS (7/7) | `src/App.test.tsx`: 7/7 Vitest + Testing Library tests passed (contract activation, utility tariffs & meter readings display, maintenance ticket technician assignment, RBAC `STAFF` restricted navigation, `OWNER` members & audit log view, and `TENANT` portal rendering & maintenance request submission). |
| Frontend production build (`npm --prefix frontend run build`) | PASS | `tsc -b && vite build` exited 0 (`dist/index.html`, CSS/JS bundles produced). |
| Docker Compose build & runtime (`POSTGRES_PORT=55432 FRONTEND_PORT=55173 docker compose up --build -d --wait`) | PASS | `postgres`, `backend`, and `frontend` containers all `Up (healthy)`. `GET /actuator/health` returned `200 {"status":"UP"}`. `GET /api/auth/csrf` via Nginx proxy returned `200` with `XSRF-TOKEN` cookie. |
| PostgreSQL 16 + Flyway migrations | PASS (5/5) | `flyway_schema_history` verified `1` (`identity property room`), `2` (`tenant contract`), `3` (`utility billing payments`), `4` (`maintenance and handover`), and `5` (`rbac audit settings recurring`) all `success = t`. |
| Real browser E2E QA (`http://127.0.0.1:55173/` via headless Chromium) | PASS | Verified full P0 + P1 + V5 user journeys against live PostgreSQL + backend + Nginx SPA: Owner registration, reload session persistence, organization settings update (`100k` internet + `50k` garbage + `120k` parking), property & room `A201` creation, tenant creation + portal provisioning, contract activation, tariff & meter reading, draft invoice generation (`4.620.000 ₫`) + draft discount (`-50.000 ₫` -> `4.570.000 ₫`) + issue + full payment (`UNC-V5-001`), `STAFF` invite & login RBAC menu isolation, `TENANT` portal login (`CỔNG NGƯỜI THUÊ`) + invoice view + ticket submission + self-service password change & re-login, Owner `Bảo trì` verification & immutable `Nhật ký` (`AuditLog`) inspection, and clean logout. Zero console errors, zero uncaught page errors, and zero backend error/warn stacktraces. |

## Test scope notes

- Backend integration suite runs against H2 in PostgreSQL compatibility mode (`application-test.yml`) for fast CI-free iteration; schema migrations `V1`–`V5` and runtime behavior were verified directly against PostgreSQL 16 inside Docker Compose.
- Host ports `5432` and `5173` were occupied by pre-existing services on the host during verification, so `POSTGRES_PORT=55432` and `FRONTEND_PORT=55173` were used without touching host services.
