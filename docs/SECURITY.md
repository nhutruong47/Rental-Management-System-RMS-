# Security Architecture & Review

## Controls Implemented

1. **Password Security**: `BCryptPasswordEncoder(12)` (`SecurityConfiguration.java`). Minimum password length 12 characters (`@Size(min = 12, max = 72)`). Self-service password updates require verifying `currentPassword` (`POST /api/auth/change-password`).
2. **Same-Origin Session + CSRF**:
   - Session cookie `RMSSESSION` configured with `http-only: true`, `same-site: lax`, and configurable `SESSION_COOKIE_SECURE`.
   - `CookieCsrfTokenRepository.withHttpOnlyFalse()` paired with `CsrfTokenRequestAttributeHandler` and frontend `X-XSRF-TOKEN` header injection.
3. **RBAC & Method-Level Authorization**:
   - `@EnableMethodSecurity` enabled in `SecurityConfiguration.java`; all 12 REST controllers enforce role checks via `@PreAuthorize` across `OWNER`, `MANAGER`, `STAFF`, and `TENANT`.
   - `TenantPortalController` (`/api/portal/**`) restricts `TENANT` principals strictly to contracts, invoices, and maintenance tickets linked to their own `tenants.user_id`.
4. **Brute-Force Protection**:
   - `AuthRateLimiter` caps failed login attempts at 10 per 60-second window per `clientIp|email`, returning `429 Too Many Requests` on lockout and resetting on successful login.
5. **Immutable Audit Trail**:
   - `AuditService` records security-relevant and financial mutations (`OWNER_REGISTERED`, `LOGIN`, `PASSWORD_CHANGED`, `MEMBER_INVITED`, `MEMBER_ROLE_UPDATED`, `MEMBER_REMOVED`, `SETTINGS_UPDATED`, `TENANT_PORTAL_PROVISIONED`, `INVOICE_ADJUSTED`, `INVOICE_ISSUED`, `PAYMENT_RECORDED`) into `audit_logs`, readable only by `OWNER` and `MANAGER`.
6. **Tenant / Organization Isolation**:
   - `organizationId` and `userId` are extracted solely from `@AuthenticationPrincipal RmsUserPrincipal`.
   - Every repository query filters by `organizationId`, reinforced by composite foreign keys `(organization_id, entity_id)` in Flyway DDL.
   - Cross-organization reads and mutations consistently return `404 Not Found` (preventing resource existence enumeration) or `[]` on list endpoints.
7. **Safe Error Handling & Data Minimization**:
   - `server.error.include-message: never` prevents internal stack traces from leaking.
   - `DispatcherType.ERROR` is permitted in Spring Security so validation/CSRF/conflict/rate-limit responses preserve `400`/`403`/`409`/`429` instead of converting to `401`.
   - Tenant profiles store only `fullName`, `phone`, and `email`; ID card numbers/scans and file uploads are intentionally omitted.
   - No hardcoded production secrets (`application.yml` and `docker-compose.yml` use environment variables with local-only development fallbacks).

## Security Verification Summary

- Verified via automated tests in `RentalManagementApiIntegrationTest`:
  - Anonymous access denial (`401`), missing CSRF denial (`403`), cross-organization isolation (`404`/`[]`), `STAFF`/`TENANT` `@PreAuthorize` denial (`403`), `TENANT` cross-tenant portal IDOR isolation, login rate limiting (`429`), and password change flow.
  - Session invalidation verified via browser E2E (`POST /api/auth/logout` → `GET /api/auth/me` returns `401`).
- Remaining hardening items for internet-facing multi-node production: enable `SESSION_COOKIE_SECURE=true` behind TLS and back rate limiting with a distributed store (e.g. Redis / reverse proxy) if deploying multiple backend replicas.
