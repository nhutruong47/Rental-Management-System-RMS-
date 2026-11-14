ALTER TABLE rental_contracts
    ADD COLUMN deposit_amount NUMERIC(19, 0) NOT NULL DEFAULT 0;

ALTER TABLE rental_contracts
    ADD CONSTRAINT ck_contract_deposit_nonnegative CHECK (deposit_amount >= 0);

CREATE TABLE handover_records (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    contract_id UUID NOT NULL,
    terminated_date DATE NOT NULL,
    final_electricity_reading NUMERIC(19, 3),
    final_water_reading NUMERIC(19, 3),
    outstanding_balance NUMERIC(19, 0) NOT NULL,
    deposit_held NUMERIC(19, 0) NOT NULL,
    deposit_deducted NUMERIC(19, 0) NOT NULL,
    deposit_refunded NUMERIC(19, 0) NOT NULL,
    checklist_notes VARCHAR(1000),
    recorded_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_handover_contract_org FOREIGN KEY (organization_id, contract_id)
        REFERENCES rental_contracts(organization_id, id),
    CONSTRAINT uk_handover_contract UNIQUE (contract_id),
    CONSTRAINT ck_handover_amounts CHECK (
        outstanding_balance >= 0
        AND deposit_held >= 0
        AND deposit_deducted >= 0
        AND deposit_refunded >= 0
        AND deposit_deducted + deposit_refunded = deposit_held
    )
);

CREATE INDEX ix_handover_org_created ON handover_records(organization_id, created_at DESC);

CREATE TABLE maintenance_tickets (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    room_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    assignee_name VARCHAR(120),
    estimated_cost NUMERIC(19, 0) NOT NULL DEFAULT 0,
    actual_cost NUMERIC(19, 0) NOT NULL DEFAULT 0,
    resolution_notes VARCHAR(1000),
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_ticket_room_org FOREIGN KEY (organization_id, room_id)
        REFERENCES rooms(organization_id, id),
    CONSTRAINT ck_ticket_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT ck_ticket_status CHECK (status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'RESOLVED', 'CLOSED', 'REOPENED')),
    CONSTRAINT ck_ticket_costs_nonnegative CHECK (estimated_cost >= 0 AND actual_cost >= 0)
);

CREATE INDEX ix_tickets_org_status ON maintenance_tickets(organization_id, status, created_at DESC);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    event_type VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(500) NOT NULL,
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_notifications_org_created ON notifications(organization_id, created_at DESC);
