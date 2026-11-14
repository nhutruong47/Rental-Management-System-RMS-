# Security Architecture & Review

## Controls Implemented

1. **Password Security**: `BCryptPasswordEncoder(12)` (`SecurityConfiguration.java`). Minimum password length 12 characters (`@Size(min = 12, max = 72)`).
2. **Same-Origin Session + CSRF**:
   - Session cookie `RMSSESSION` configured with `http-only: true`, `same-site: lax`, and configurable `SESSION_COOKIE_SECURE`.
   - `CookieCsrfTokenRepository.withHttpOnlyFalse()` paired with `CsrfTokenRequestAttributeHandler` and frontend `X-XSRF-TOKEN` header injection.
3. **Tenant / Organization Isolation**:
   - `organizationId` is extracted solely from `@AuthenticationPrincipal RmsUserPrincipal` populated at authentication from `organization_memberships`.
   - Every repository query filters by `organizationId`, reinforced by composite foreign keys `(organization_id, entity_id)` in Flyway DDL.
   - Cross-organization reads and mutations consistently return `404 Not Found` (preventing resource existence enumeration) or `[]` on list endpoints.
4. **Safe Error Handling**:
   - `server.error.include-message: never` prevents internal stack traces from leaking.
   - `DispatcherType.ERROR` is permitted in Spring Security so validation/CSRF/conflict responses preserve `400`/`403`/`409` instead of converting to `401`.
   - `ApiExceptionHandler` returns structured JSON `{ "error": "..." }` for `ResponseStatusException`, validation errors, optimistic lock conflicts, and integrity violations.
5. **Sensitive Data Minimization**:
   - Tenant profiles store only `fullName`, `phone`, and `email`; ID card numbers/scans and file uploads are intentionally omitted.
6. **No Hardcoded Production Secrets**:
   - `application.yml` and `docker-compose.yml` use environment variables with local-only development fallbacks (`local-rms-password`).

## Security Verification Summary

- Verified via automated tests in `RentalManagementApiIntegrationTest`:
  - Anonymous access denial (`401` on `/api/properties`, `/api/auth/me`).
  - Missing CSRF denial (`403 Forbidden` on `POST /api/auth/register`).
  - Cross-organization isolation verified across properties, tenants, utility tariffs, invoices/payments, contract handovers, and maintenance tickets.
  - Session invalidation verified via browser E2E (`POST /api/auth/logout` → `GET /api/auth/me` returns `401`).
- Remaining hardening items for internet-facing production: enable `SESSION_COOKIE_SECURE=true` behind TLS, add login rate limiting at reverse proxy/gateway, and add email-based password reset + multi-role RBAC enforcement if adding `MANAGER`/`STAFF`/`TENANT` logins.
