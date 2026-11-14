CREATE TABLE utility_tariffs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    utility_type VARCHAR(20) NOT NULL,
    price_per_unit NUMERIC(19, 4) NOT NULL,
    effective_from DATE NOT NULL,
    CONSTRAINT ck_tariff_type CHECK (utility_type IN ('ELECTRICITY', 'WATER')),
    CONSTRAINT ck_tariff_price_nonnegative CHECK (price_per_unit >= 0),
    CONSTRAINT uk_tariff_org_type_effective UNIQUE (organization_id, utility_type, effective_from)
);

CREATE INDEX ix_tariffs_org_type_effective ON utility_tariffs(organization_id, utility_type, effective_from DESC);

CREATE TABLE meter_readings (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    room_id UUID NOT NULL,
    utility_type VARCHAR(20) NOT NULL,
    billing_period VARCHAR(7) NOT NULL,
    previous_reading NUMERIC(19, 3) NOT NULL,
    current_reading NUMERIC(19, 3) NOT NULL,
    unit_price NUMERIC(19, 4) NOT NULL,
    charge NUMERIC(19, 0) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reading_room_org FOREIGN KEY (organization_id, room_id) REFERENCES rooms(organization_id, id),
    CONSTRAINT ck_reading_type CHECK (utility_type IN ('ELECTRICITY', 'WATER')),
    CONSTRAINT ck_reading_period CHECK (billing_period ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    CONSTRAINT ck_reading_monotonic CHECK (current_reading >= previous_reading),
    CONSTRAINT ck_reading_price_nonnegative CHECK (unit_price >= 0),
    CONSTRAINT ck_reading_charge_nonnegative CHECK (charge >= 0),
    CONSTRAINT uk_reading_room_utility_period UNIQUE (room_id, utility_type, billing_period)
);

CREATE INDEX ix_readings_org_period ON meter_readings(organization_id, billing_period);

ALTER TABLE rental_contracts
    ADD CONSTRAINT uk_contract_org_id UNIQUE (organization_id, id);

CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    contract_id UUID NOT NULL,
    billing_period VARCHAR(7) NOT NULL,
    rent_amount NUMERIC(19, 0) NOT NULL,
    electricity_amount NUMERIC(19, 0) NOT NULL DEFAULT 0,
    water_amount NUMERIC(19, 0) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19, 0) NOT NULL,
    amount_paid NUMERIC(19, 0) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    issued_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_invoice_contract_org FOREIGN KEY (organization_id, contract_id)
        REFERENCES rental_contracts(organization_id, id),
    CONSTRAINT ck_invoice_period CHECK (billing_period ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    CONSTRAINT ck_invoice_amounts CHECK (rent_amount >= 0 AND electricity_amount >= 0 AND water_amount >= 0
        AND total_amount = rent_amount + electricity_amount + water_amount AND amount_paid >= 0 AND amount_paid <= total_amount),
    CONSTRAINT ck_invoice_status CHECK (status IN ('DRAFT', 'ISSUED', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'VOID')),
    CONSTRAINT uk_invoice_org_contract_period UNIQUE (organization_id, contract_id, billing_period)
);

CREATE INDEX ix_invoice_org_period_status ON invoices(organization_id, billing_period, status);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    amount NUMERIC(19, 0) NOT NULL,
    method VARCHAR(24) NOT NULL,
    reference VARCHAR(120) NOT NULL,
    idempotency_key VARCHAR(80) NOT NULL,
    recorded_by_user_id UUID NOT NULL REFERENCES users(id),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_payment_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_payment_method CHECK (method IN ('CASH', 'BANK_TRANSFER')),
    CONSTRAINT uk_payment_org_idempotency UNIQUE (organization_id, idempotency_key)
);

CREATE INDEX ix_payments_invoice ON payments(invoice_id, recorded_at);
