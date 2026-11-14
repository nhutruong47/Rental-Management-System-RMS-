# Known Limitations

1. **Single-Role Operator Flow Active**: `OWNER` registration and authenticated workspace are live; `MANAGER` / `STAFF` account invitation UI and `TENANT` self-service portal (`FR-014`) remain deferred.
2. **Manual Payment Confirmation Only**: Cash and bank transfers are recorded manually by the operator with reference notes and idempotency keys; no automated bank webhook or payment gateway reconciliation exists.
3. **No Tax E-Invoice or Legal E-Signature**: Issued invoices are internal payment statements (not Vietnamese e-tax invoices / hóa đơn điện tử), and contracts represent internal operational records requiring manual signing and qualified legal review (`I-005`).
4. **Simple Flat-Rate Utility Tariffs**: Electricity and water use operator-configured flat unit rates (`effectiveFrom`) rather than tiered EVN brackets, shared meters, or mid-period proration.
5. **Deferred Convenience Features**: PDF invoice export, automated email/SMS reminders, password reset via email, and multi-language switching are not part of this release.
6. **Environment Port Overrides**: On hosts with existing PostgreSQL (`5432`) or Vite (`5173`) services, run Compose with `POSTGRES_PORT` and `FRONTEND_PORT` overrides.
