# Test Plan

## 1. Backend Integration Suite (`RentalManagementApiIntegrationTest`)
Executed via Dockerized Maven (`maven:3.9.9-eclipse-temurin-21`) with Spring Boot `@SpringBootTest` + `MockMvc` in H2 PostgreSQL compatibility mode:
- Authentication, registration conflict (`409`), invalid credentials (`401`), `/api/auth/me`, missing CSRF (`403`), validation errors (`400`), unauthenticated `/actuator/health` (`200`).
- Cross-organization isolation for properties, tenants, utility tariffs, invoices, payments, handovers, and maintenance tickets.
- Room code uniqueness (`409`), negative rent/tariff/reading rejection (`400`), manual room status lock while `OCCUPIED` (`409`).
- Contract draft creation (rent + deposit snapshot), activation overlap rejection (`409`), termination precondition enforcement (unpaid invoice `409`, excessive deposit deduction `400`), handover persistence, and room release (`AVAILABLE`).
- Chronological meter reading enforcement, duplicate reading rejection (`409`), VND calculation, invoice idempotency, partial payment + idempotency key mismatch (`409`) + overpayment rejection (`400`).
- Maintenance ticket state transitions (`OPEN` → `ASSIGNED` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`, invalid jump `409`).
- End-to-end `@Transactional(propagation = Propagation.NOT_SUPPORTED)` test verifying zero `LazyInitializationException` under `spring.jpa.open-in-view: false`.

## 2. Frontend Automated Tests & Static Checks
- `npm --prefix frontend run check` (`tsc -b --pretty false`)
- `npm --prefix frontend run test -- --reporter=verbose` (Vitest + Testing Library + jsdom)
- `npm --prefix frontend run build` (`tsc -b && vite build`)

## 3. Full-Stack Compose & Browser E2E Verification
- `POSTGRES_PORT=55432 FRONTEND_PORT=55173 docker compose up --build -d`
- Verify all 3 container healthchecks, PostgreSQL 16 Flyway `flyway_schema_history` (`V1`–`V4`), Nginx reverse proxy, and headless Chromium execution of the complete operator flow (register → reload → property/room → tenant → contract activate → tariff/reading → invoice/issue/partial pay → maintenance ticket → settle invoice & terminate/handover → dashboard & notifications → logout).
