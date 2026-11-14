# Test results

Updated: 2026-11-14

## Verified test & build runs

| Check | Result | Evidence / notes |
|---|---|---|
| Backend integration suite (`mvn -B test` via `maven:3.9.9-eclipse-temurin-21`) | PASS (18/18) | `RentalManagementApiIntegrationTest`: 18 tests run, 0 failures, 0 errors, 0 skipped. Includes auth/CSRF/session, `/api/auth/me`, `/actuator/health`, cross-org isolation, property/room uniqueness & validation, contract activation & overlap rejection, tariff list/create, monotonic meter reading order, invoice generation/issue/snapshot, idempotent partial payment & overpayment guard, 403 missing CSRF, 400 validation JSON, and full workflow with `@Transactional(propagation = Propagation.NOT_SUPPORTED)` matching `open-in-view: false`. |
| Frontend TypeScript check (`npm --prefix frontend run check`) | PASS | `tsc -b --pretty false` exited 0 with no diagnostics. |
| Frontend UI tests (`npm --prefix frontend run test -- --reporter=verbose`) | PASS (2/2) | `src/App.test.tsx`: 2/2 Vitest + Testing Library tests passed (contract list/activation and utility tariffs + meter readings display). |
| Frontend production build (`npm --prefix frontend run build`) | PASS | `tsc -b && vite build` exited 0 (`dist/index.html`, CSS/JS bundles produced). |
| Docker Compose build & runtime (`POSTGRES_PORT=55432 FRONTEND_PORT=55173 docker compose up --build -d`) | PASS | `postgres`, `backend`, and `frontend` containers all `Up (healthy)`. `GET /actuator/health` returned `200 {"status":"UP"}`. `GET /api/auth/csrf` via Nginx proxy returned `200` with `XSRF-TOKEN` cookie. |
| PostgreSQL 16 + Flyway migrations | PASS (3/3) | `flyway_schema_history` verified `1` (`identity property room`), `2` (`tenant contract`), `3` (`utility billing payments`) all `success = t`. |
| Real browser E2E QA (`http://127.0.0.1:55173/` via headless Chromium) | PASS | Verified full user journey against live PostgreSQL + backend + Nginx SPA: owner registration & auto-login, page reload session restore via `/api/auth/me`, property & room creation, tenant creation, draft contract creation & activation (`AVAILABLE` -> `OCCUPIED`), electricity & water tariff configuration, meter reading entry & table rendering, invoice generation (`3,925,000 ₫`) & issue, partial manual payment (`1,500,000 ₫` -> `2,425,000 ₫` balance due), real dashboard metrics (`100%` occupancy, exact VND totals), and logout session invalidation (`/api/auth/me` -> `401`). Zero console errors; zero unexpected HTTP >=400 errors. |

## Test scope notes

- Backend integration suite runs against H2 in PostgreSQL compatibility mode (`application-test.yml`) for fast CI-free iteration; schema migrations and runtime behavior were additionally verified directly against PostgreSQL 16 inside Docker Compose.
- Host ports `5432` and `5173` were occupied by pre-existing services on the host during verification, so `POSTGRES_PORT=55432` and `FRONTEND_PORT=55173` were used without touching host services.
