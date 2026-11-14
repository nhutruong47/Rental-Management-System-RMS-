# Rental Management System (RMS)

Vietnamese web platform for managing residential rental properties, rooms, tenants, exclusive contracts, configurable utility tariffs & meter readings, VND invoices with recurring fees & adjustments, manual payments, maintenance tickets, contract handovers, in-app notifications, organization RBAC, audit logs, and a dedicated tenant self-service portal.

## Current status

Implemented and verified end-to-end (`VERIFIED RUNNABLE — V1–V5 FULL RELEASE`) for all P0 core workflows (`WF-001`–`WF-004`), P1 operations (`WF-005` maintenance, `WF-006` contract termination & deposit settlement handover, notifications), and V5 capabilities (`OWNER`/`MANAGER`/`STAFF`/`TENANT` RBAC, Tenant Portal `FR-014`, Organization Settings recurring fees, draft invoice adjustments, login rate limiter, self-service password change, and immutable audit logs). See [docs/FINAL_DELIVERY_REPORT.md](docs/FINAL_DELIVERY_REPORT.md), [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md), [docs/TEST_RESULTS.md](docs/TEST_RESULTS.md), and [docs/KNOWN_LIMITATIONS.md](docs/KNOWN_LIMITATIONS.md).

## Stack

- **Backend:** Java 21, Spring Boot 3.5.16, Spring Security (`@EnableMethodSecurity` + `@PreAuthorize`, `RMSSESSION` HttpOnly cookie + `X-XSRF-TOKEN` CSRF, BCrypt/Argon2 password hashing, `AuthRateLimiter`), Spring Data JPA (`open-in-view: false`), Flyway (`V1`–`V5`), PostgreSQL 16, Spring Boot Actuator (`/actuator/health`).
- **Frontend:** React 19, TypeScript, Vite, Nginx reverse proxy (`/api/` and `/actuator/`). Includes full Vietnamese operator workspace (`OWNER` / `MANAGER` / `STAFF`) and dedicated **Cổng người thuê** (`TENANT`).
- **Local integration:** Docker Compose (local/dev only; no public deployment, payment gateway, e-signature, or tax e-invoice integration).

## Local development

Run from the repository root:

```bash
docker compose up --build -d
```

If host ports `5432` or `5173` are occupied on your machine, override them explicitly:

```bash
POSTGRES_PORT=55432 FRONTEND_PORT=55173 docker compose up --build -d --wait
```

- Frontend SPA: `http://localhost:5173/` (or `http://localhost:${FRONTEND_PORT}/`)
- Backend health: `http://localhost:8080/actuator/health`
- **Development accounts:** No hardcoded credentials are seeded. Click **"Tạo tài khoản"** on the login screen (password ≥ 12 characters) to register an isolated development organization and `OWNER` account, then invite `MANAGER`/`STAFF` members or provision `TENANT` portal accounts from the UI.

## Running tests

Backend integration tests (using Dockerized Maven + Java 21 with H2 PostgreSQL compatibility mode):

```bash
docker run --rm -v "$(pwd)/backend:/workspace" -v "$HOME/.m2-rms:/root/.m2" -w /workspace maven:3.9.9-eclipse-temurin-21 mvn -B test
```

Frontend type-check, Vitest UI suite, and production build:

```bash
npm --prefix frontend run check
npm --prefix frontend run test -- --reporter=verbose
npm --prefix frontend run build
```

## Documentation index

- [Final Delivery Report](docs/FINAL_DELIVERY_REPORT.md)
- [BRD](docs/BRD.md) · [SRS](docs/SRS.md) · [Business Rules](docs/BUSINESS_RULES.md) · [Use Cases](docs/USE_CASES.md) · [Workflows](docs/WORKFLOWS.md)
- [Architecture](docs/ARCHITECTURE.md) · [ERD](docs/ERD.md) · [API](docs/API.md) · [Security](docs/SECURITY.md)
- [Project Status](docs/PROJECT_STATUS.md) · [Requirements Traceability](docs/REQUIREMENTS_TRACEABILITY.md) · [Test Plan](docs/TEST_PLAN.md) · [Test Results](docs/TEST_RESULTS.md)
- [Deployment](docs/DEPLOYMENT.md) · [Assumptions](docs/ASSUMPTIONS.md) · [Decisions](docs/DECISIONS.md) · [Issues](docs/ISSUES.md) · [Known Limitations](docs/KNOWN_LIMITATIONS.md)
