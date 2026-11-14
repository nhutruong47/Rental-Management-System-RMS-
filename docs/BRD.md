# Business Requirements Document (BRD) — Rental Management System (RMS)

## 1. Product Vision

Rental Management System (RMS) is a Vietnamese-market web platform enabling small and mid-sized rental property operators (nhà trọ, căn hộ dịch vụ, chung cư mini) to manage properties, rooms, tenants, exclusive rental contracts, configurable utility billing, manual payments, maintenance tickets, and contract handovers in one organization-isolated workspace.

## 2. Business Objectives

- Replace ad-hoc spreadsheets with deterministic VND billing (`BigDecimal`, integer đồng rounding) and immutable issued invoice snapshots.
- Prevent double-booking of exclusive rooms via transactional contract activation and date-overlap validation.
- Enforce strict organization-level data isolation derived from authenticated server-side membership.
- Track maintenance requests (cost & resolution lifecycle) and contract termination with deposit settlement and handover checklists.
- Provide real-time scoped occupancy and financial metrics (billed vs collected vs outstanding debt, distinct from held security deposits).

## 3. Stakeholders & Actors

| Actor | Role in RMS | Current MVP Implementation Status |
|---|---|---|
| **OWNER** | Registers organization, manages all properties, rooms, tenants, contracts, tariffs, readings, invoices, payments, maintenance, handovers | **Implemented & verified** |
| **MANAGER / STAFF** | Operational role definitions (`MembershipRole` enum) | Schema & enum present; invitation/UI RBAC expansion deferred to post-MVP |
| **TENANT** | Subject of rental contract & billing records | Managed by operator; self-service portal deferred to post-MVP |
| **SUPER_ADMIN** | SaaS platform operator | Multi-tenant SaaS billing/admin deferred to post-MVP |

## 4. Scope Boundaries

- **Included (Implemented & Verified):** P0 core rental lifecycle + P1 maintenance tickets (`WF-005`), contract termination & deposit settlement handover (`WF-006`), room operational status toggle, and in-app activity notifications.
- **Excluded / Deferred:** Payment gateways & bank webhooks, legally binding e-signatures, official e-tax invoice integration (hóa đơn điện tử), automated SMS/email delivery, PDF export, tenant self-service portal, staff invitation UI, and SaaS subscription billing.
