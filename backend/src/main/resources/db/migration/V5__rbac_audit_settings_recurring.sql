ALTER TABLE organization_memberships
    DROP CONSTRAINT ck_membership_role;

ALTER TABLE organization_memberships
    ADD CONSTRAINT ck_membership_role CHECK (role IN ('OWNER', 'MANAGER', 'STAFF', 'TENANT'));

ALTER TABLE tenants
    ADD COLUMN user_id UUID REFERENCES users(id);

CREATE UNIQUE INDEX uk_tenants_user_id ON tenants(user_id) WHERE user_id IS NOT NULL;

ALTER TABLE invoices
    DROP CONSTRAINT ck_invoice_amounts;

ALTER TABLE invoices
    ADD COLUMN service_amount NUMERIC(19, 0) NOT NULL DEFAULT 0;

ALTER TABLE invoices
    ADD COLUMN discount_amount NUMERIC(19, 0) NOT NULL DEFAULT 0;

ALTER TABLE invoices
    ADD COLUMN adjustment_notes VARCHAR(300);

ALTER TABLE invoices
    ADD CONSTRAINT ck_invoice_amounts CHECK (
        rent_amount >= 0
        AND electricity_amount >= 0
        AND water_amount >= 0
        AND service_amount >= 0
        AND discount_amount >= 0
        AND total_amount = GREATEST(0, rent_amount + electricity_amount + water_amount + service_amount - discount_amount)
        AND amount_paid >= 0
        AND amount_paid <= total_amount
    );

CREATE TABLE organization_settings (
    organization_id UUID PRIMARY KEY REFERENCES organizations(id),
    billing_due_day SMALLINT NOT NULL DEFAULT 5,
    internet_fee NUMERIC(19, 0) NOT NULL DEFAULT 0,
    garbage_fee NUMERIC(19, 0) NOT NULL DEFAULT 0,
    parking_fee NUMERIC(19, 0) NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_settings_due_day CHECK (billing_due_day BETWEEN 1 AND 28),
    CONSTRAINT ck_settings_fees_nonnegative CHECK (internet_fee >= 0 AND garbage_fee >= 0 AND parking_fee >= 0)
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    actor_user_id UUID REFERENCES users(id),
    actor_email VARCHAR(254) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id VARCHAR(64),
    detail VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_audit_logs_org_created ON audit_logs(organization_id, created_at DESC);
