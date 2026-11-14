# Rental Management System (RMS)

MVP web platform for managing small rental properties in Vietnam (properties, rooms, tenants, exclusive contracts, configurable utility tariffs & meter readings, VND invoices, manual payment recording, and owner dashboard).

## Current status

Core P0 MVP workflows are implemented and verified end-to-end against PostgreSQL + Flyway + Docker Compose. See [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md), [docs/TEST_RESULTS.md](docs/TEST_RESULTS.md), and [docs/ISSUES.md](docs/ISSUES.md) for verified details, limitations, and deferred P1 scope.

## Stack

- **Backend:** Java 21, Spring Boot 3.5.16, Spring Security (session cookie `RMSSESSION` + CSRF `X-XSRF-TOKEN`), Spring Data JPA (`open-in-view: false`), Flyway, PostgreSQL 16, Spring Boot Actuator (`/actuator/health`).
- **Frontend:** React, TypeScript, Vite, Nginx reverse proxy (`/api/` and `/actuator/`).
- **Local integration:** Docker Compose (development only; no public deployment, e-signature, tax e-invoice, or payment gateway integration).

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
