# System Architecture

## 1. Architectural Style

RMS is a **Modular Monolith** built for single-host or containerized deployment via Docker Compose:

- **Frontend (`frontend/`)**: React 19 + TypeScript + Vite SPA served by Nginx (`1.27-alpine`). Nginx serves static assets with SPA fallback and reverse-proxies `/api/` and `/actuator/` to `backend:8080`, keeping cookies and CSRF tokens strictly same-origin.
- **Backend (`backend/`)**: Spring Boot 3.5.16 on Java 21 LTS with `spring.jpa.open-in-view: false`. All controllers delegate to `@Transactional` services that materialize explicit Java `record` DTOs before leaving the transaction boundary.
- **Database (`postgres`)**: PostgreSQL 16 (`postgres:16-alpine`), schema owned exclusively by Flyway SQL migrations `V1`–`V4` (`hibernate.ddl-auto: validate`).

## 2. Backend Module Layout (`vn.rms.*`)

| Package | Responsibility |
|---|---|
| `vn.rms.identity` | User registration, login/logout, `/api/auth/me`, `/api/auth/csrf`, `RmsUserPrincipal`, `AccountUserDetailsService` |
| `vn.rms.config` | Spring Security filter chain (`RMSSESSION`, `CookieCsrfTokenRepository`, `DispatcherType.ERROR` permit, BCrypt 12) |
| `vn.rms.property` | `Property`, `Room`, room status transitions, org-scoped queries |
| `vn.rms.tenant` | `Tenant` contact directory |
| `vn.rms.contract` | `RentalContract`, `HandoverRecord`, activation overlap checks, termination & deposit settlement |
| `vn.rms.utility` | `UtilityTariff`, `MeterReading`, chronological monotonic consumption & VND charge calculation |
| `vn.rms.billing` | `RentalInvoice`, `ManualPayment`, idempotent invoice generation & manual payment allocation |
| `vn.rms.maintenance` | `MaintenanceTicket`, `TicketStatus` state machine, assignment & cost tracking |
| `vn.rms.notification` | `Notification` persistence and read-state management |
| `vn.rms.shared` | `ApiExceptionHandler` mapping validation, optimistic locking, integrity violations, and `ResponseStatusException` to `{ "error": ... }` |
