# Rental Management System (RMS)

MVP platform for managing rental properties in Vietnam. The repository was confirmed empty on GitHub and had no local source code or project status files when work resumed; implementation starts from that verified state, preserving existing unrelated repositories.

## Current status

See [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md) for the verified baseline, priorities and exact next action. This project is under active development. Do not treat planned workflows as implemented until their code, tests and integrated execution are verified.

## Planned stack

- Backend: Java 21, Spring Boot, Spring Security, Spring Data JPA, Flyway, PostgreSQL.
- Frontend: React, TypeScript and Vite, integrated with the real backend API.
- Local integration: Docker Compose; no public deployment or real payment integration.

## Initial verified environment

- Linux aarch64; Git and Docker Compose available.
- Node and npm available; Docker daemon and PostgreSQL images available.
- Java/Maven are not installed on the host; Java 21 was verified in Docker. Build and test through the Maven/Temurin container until a wrapper is committed.
- Repository was empty at `https://github.com/nhutruong47/Rental-Management-System-RMS-` at discovery. No application build or workflow can be claimed as passing yet.

## Local development

Run from the repository root:

```bash
docker compose up --build
```

The compose command will become the supported integrated run command as implementation lands. Check `docs/PROJECT_STATUS.md` for what is currently runnable and `docs/TEST_RESULTS.md` for actual test executions.

# Rental-Management-System-RMS-
