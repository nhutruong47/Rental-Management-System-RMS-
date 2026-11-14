# Test results

Updated: 2026-11-14

## Verified test & build runs

| Check | Result | Evidence / notes |
|---|---|---|
| Backend integration suite (`mvn -B test` via `maven:3.9.9-eclipse-temurin-21`) | PASS (21/21) | `RentalManagementApiIntegrationTest`: 21 tests run, 0 failures, 0 errors, 0 skipped. Includes auth/CSRF/session, `/api/auth/me`, `/actuator/health`, cross-org isolation, property/room uniqueness & status lock (`409` when `OCCUPIED`), contract activation & overlap rejection, tariff list/create, monotonic meter reading order, invoice generation/issue/snapshot, idempotent partial payment & overpayment guard, 403 missing CSRF, 400 validation JSON, full non-transactional workflow (`open-in-view: false`), `WF-006` termination/deposit settlement/handover, `WF-005` maintenance state transitions, and org-scoped notifications. |
| Frontend TypeScript check (`npm --prefix frontend run check`) | PASS | `tsc -b --pretty false` exited 0 with no diagnostics. |
| Frontend UI tests (`npm --prefix frontend run test -- --reporter=verbose`) | PASS (3/3) | `src/App.test.tsx`: 3/3 Vitest + Testing Library tests passed (contract activation, utility tariffs & meter readings display, maintenance ticket technician assignment). |
| Frontend production build (`npm --prefix frontend run build`) | PASS | `tsc -b && vite build` exited 0 (`dist/index.html`, CSS/JS bundles produced). |
| Docker Compose build & runtime (`POSTGRES_PORT=55432 FRONTEND_PORT=55173 docker compose up --build -d`) | PASS | `postgres`, `backend`, and `frontend` containers all `Up (healthy)`. `GET /actuator/health` returned `200 {"status":"UP"}`. `GET /api/auth/csrf` via Nginx proxy returned `200` with `XSRF-TOKEN` cookie. |
| PostgreSQL 16 + Flyway migrations | PASS (4/4) | `flyway_schema_history` verified `1` (`identity property room`), `2` (`tenant contract`), `3` (`utility billing payments`), `4` (`maintenance and handover`) all `success = t`. |
| Real browser E2E QA (`http://127.0.0.1:55173/` via headless Chromium) | PASS | Verified full P0 + P1 user journeys against live PostgreSQL + backend + Nginx SPA: registration, session reload persistence, property/room CRUD & status toggle, tenant creation, contract activate, utility tariffs & readings, invoice generation/issue/payment, maintenance ticket `OPEN` → `ASSIGNED` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`, contract termination & deposit settlement (`4,000,000 - 300,000 = 3,700,000 ₫`) releasing room back to `Trống`, in-app notification read, and logout. Final regression check completed with `CONSOLE_ERRORS: []` and `HTTP_ERRORS: []`. |

## Test scope notes

- Backend integration suite runs against H2 in PostgreSQL compatibility mode (`application-test.yml`) for fast CI-free iteration; schema migrations and runtime behavior were additionally verified directly against PostgreSQL 16 inside Docker Compose.
- Host ports `5432` and `5173` were occupied by pre-existing services on the host during verification, so `POSTGRES_PORT=55432` and `FRONTEND_PORT=55173` were used without touching host services.
