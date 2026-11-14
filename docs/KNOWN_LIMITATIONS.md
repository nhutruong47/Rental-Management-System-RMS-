# Known Limitations

Updated: 2026-11-14

1. **In-Memory Login Rate Limiter**: `AuthRateLimiter` tracks failed login attempts per `clientIp|email` in an in-memory `ConcurrentHashMap` (10 attempts / 60s window). This protects a single backend instance; multi-replica horizontal scaling would require a shared store such as Redis.
2. **Manual Payment Confirmation Only**: Cash and bank transfers are recorded manually by the operator with reference notes and idempotency keys; no automated bank webhook or payment gateway reconciliation exists.
3. **No Tax E-Invoice or Legal E-Signature**: Issued invoices are internal payment statements (not Vietnamese e-tax invoices / hóa đơn điện tử), and contracts represent internal operational records requiring manual signing and qualified legal review (`I-005`).
4. **Simple Flat-Rate Utility Tariffs**: Electricity and water use operator-configured flat unit rates (`effectiveFrom`) rather than tiered EVN brackets, shared meters, or mid-period proration.
5. **No Outbound Email/SMS Dispatch**: Member invitations (`MANAGER`/`STAFF`) and `TENANT` portal provisioning assign initial credentials via the owner/manager UI without automated email/SMS delivery; forgot-password shows an operational contact notice and users change passwords post-login via `/api/auth/change-password`.
6. **Deferred Convenience Features**: PDF invoice export and multi-language switching are not part of this release.
7. **Environment Port Overrides**: On hosts with existing PostgreSQL (`5432`) or Vite (`5173`) services, run Compose with `POSTGRES_PORT` and `FRONTEND_PORT` overrides.
