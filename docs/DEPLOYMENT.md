# Local Deployment & Operations Guide

## Prerequisites
- Docker Engine & Docker Compose v2
- Optional for frontend local checks: Node.js 22 LTS + npm

## Starting the Full Stack
From the repository root:

```bash
docker compose up --build -d
```

If ports `5432` (PostgreSQL) or `5173` (Frontend) are occupied on the host:

```bash
POSTGRES_PORT=55432 FRONTEND_PORT=55173 docker compose up --build -d
```

## Health & Schema Verification
- Check containers: `docker compose ps --all` (all 3 services `healthy`)
- Check backend health: `curl http://127.0.0.1:8080/actuator/health` (`{"status":"UP"}`)
- Check Flyway migrations:
  `docker compose exec -T postgres psql -U rms -d rms -c 'select version, description, success from flyway_schema_history order by installed_rank;'`

## Production Deployment Notes (Not Executed)
Before exposing to the public internet:
1. Set a strong `DB_PASSWORD` via environment/secret manager.
2. Terminate TLS at a reverse proxy and set `SESSION_COOKIE_SECURE=true`.
3. Configure reverse-proxy rate limiting on `/api/auth/login` and `/api/auth/register`.
4. Back up the `rms-postgres-data` PostgreSQL volume on a schedule.
