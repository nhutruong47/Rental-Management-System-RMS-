# Rental Management System (RMS)

Vietnamese web platform for managing small rental properties, rooms, tenants, exclusive contracts, configurable utility tariffs & meter readings, VND invoices, manual payments, maintenance tickets, contract handovers, and in-app notifications.

## Current status

Implemented and verified end-to-end (`VERIFIED RUNNABLE`) for all P0 core workflows (`WF-001`–`WF-004`) plus P1 maintenance (`WF-005`), contract termination & deposit settlement handover (`WF-006`), and in-app notifications. See [docs/FINAL_DELIVERY_REPORT.md](docs/FINAL_DELIVERY_REPORT.md), [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md), [docs/TEST_RESULTS.md](docs/TEST_RESULTS.md), and [docs/KNOWN_LIMITATIONS.md](docs/KNOWN_LIMITATIONS.md).

## Stack

- **Backend:** Java 21, Spring Boot 3.5.16, Spring Security (`RMSSESSION` HttpOnly cookie + `X-XSRF-TOKEN` CSRF), Spring Data JPA (`open-in-view: false`), Flyway (`V1`–`V4`), PostgreSQL 16, Spring Boot Actuator (`/actuator/health`).
- **Frontend:** React 19, TypeScript, Vite, Nginx reverse proxy (`/api/` and `/actuator/`).
- **Local integration:** Docker Compose (local/dev only; no public deployment, payment gateway, e-signature, or tax e-invoice integration).

## Local development

Run from the repository root:

```bash
docker compose up --build -d
```

If host ports `5432` or `5173` are occupied on your machine, override them explicitly:

```bash
POSTGRES_PORT=55432 FRONTEND_PORT=55173 docker compose up --build -d
```

- Frontend SPA: `http://localhost:5173/` (or `http://localhost:${FRONTEND_PORT}/`)
- Backend health: `http://localhost:8080/actuator/health`
- **Development accounts:** No hardcoded credentials are seeded. Click **"Tạo tài khoản"** on the login screen (password ≥ 12 characters) to register an isolated development organization and owner account.

## Running tests

Backend integration tests (using Dockerized Maven + Java 21 with H2 PostgreSQL compatibility mode):

```bash
docker run --rm -v "$(pwd)/backend:/workspace" -w /workspace maven:3.9.9-eclipse-temurin-21 mvn -B test
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
