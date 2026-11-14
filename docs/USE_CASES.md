# Use Cases Specification

| ID | Use Case | Primary Actor | Preconditions | Main Outcome |
|---|---|---|---|---|
| **UC-001** | Register Owner & Organization | Prospective Owner | Email unused, password ≥ 12 chars | Creates `UserAccount`, `Organization`, `OWNER` membership atomically |
| **UC-002** | Sign In / Restore Session / Sign Out | Authenticated User | Valid credentials + CSRF token | Issues `RMSSESSION` cookie; `/api/auth/me` restores UI state; logout invalidates session |
| **UC-003** | Manage Properties & Rooms | Owner | Authenticated session | Creates properties, unique-code rooms, toggles non-occupied room status (`AVAILABLE` ↔ `MAINTENANCE`) |
| **UC-004** | Manage Tenants | Owner | Authenticated session | Creates and lists minimal contact profiles scoped to org |
| **UC-005** | Create & Activate Contract | Owner | `AVAILABLE` room + tenant in same org | Creates `DRAFT` contract with rent/deposit snapshot; activation validates overlap & sets room `OCCUPIED` |
| **UC-006** | Configure Utility Tariffs & Readings | Owner | Org room + active tariff for period | Saves electricity/water tariffs and computes monotonic period usage & VND charge |
| **UC-007** | Generate, Issue & Pay Invoice | Owner | Active contract(s) in billing period | Generates VND invoice, issues snapshot, allocates idempotent partial/full manual payments |
| **UC-008** | Terminate Contract & Settle Deposit | Owner | `ACTIVE` contract with zero unpaid balance | Records final meter readings, deducts/refunds deposit, persists `HandoverRecord`, sets contract `TERMINATED` and room `AVAILABLE` |
| **UC-009** | Manage Maintenance Tickets | Owner | Org room | Creates ticket (`OPEN`), assigns technician (`ASSIGNED`), progresses (`IN_PROGRESS`), resolves with actual cost (`RESOLVED`), closes/reopens |
| **UC-010** | View Dashboard & Notifications | Owner | Authenticated session | Displays live org-scoped occupancy, invoice cashflow chart/totals, and read/unread notifications |
