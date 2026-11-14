# Assumptions and items for human/legal review

These assumptions are product choices, not claims about Vietnamese law. Record changes in `DECISIONS.md` and the requirements traceability if confirmed differently.

| ID | Assumption / question | MVP treatment | Review status |
|---|---|---|---|
| A-001 | A small rental operator begins with one owner-managed organization. | Owner sign-up creates one organization; staff and invitations deferred. | Product assumption |
| A-002 | A room is exclusively occupied by a single tenant/contract in MVP. | One tenant and one room per contract; overlapping occupancy rejected. | Product assumption |
| A-003 | Owner manually verifies cash/bank receipts before recording payment. | No bank API; recording user and reference/evidence note retained. | Product assumption; accounting flow needs review |
| A-004 | Electricity and water charges use operator-configured per-unit prices. | No government price is hardcoded; simple non-tiered quantity × rate for MVP. | Product assumption; legal tariff review required |
| A-005 | Rent is charged by complete billing month in MVP. | Billing is for a whole monthly period; mid-month proration deferred. | Product assumption |
| A-006 | Owner data is served through an authenticated application session. | HttpOnly cookie + CSRF-protected session; same-origin deployment. | Security decision; verify configuration |
| A-007 | VND values are whole đồng. | Amounts use decimal arithmetic and integer-VND rounding. | Product assumption |
| A-008 | Rental contract, residence reporting, privacy, utility pricing and tax-invoice rules may impose requirements not represented here. | Do not claim legal compliance or tax invoice status; obtain qualified Vietnamese review before production use. | Unverified legal questions |
| A-009 | User request says resume but provides URL for a newly created empty GitHub repository. | Treat repository as new RMS project; previous detailed prompt supplies desired product intent only, not evidence of code completion. | Verified repo state |
