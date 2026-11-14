CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(40),
    email VARCHAR(254),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_org_id UNIQUE (organization_id, id)
);

CREATE INDEX ix_tenants_org_name ON tenants(organization_id, full_name);

CREATE TABLE rental_contracts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    room_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    monthly_rent NUMERIC(19, 0) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contract_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_contract_room_org FOREIGN KEY (organization_id, room_id) REFERENCES rooms(organization_id, id),
    CONSTRAINT fk_contract_tenant_org FOREIGN KEY (organization_id, tenant_id) REFERENCES tenants(organization_id, id),
    CONSTRAINT ck_contract_dates CHECK (end_date >= start_date),
    CONSTRAINT ck_contract_rent_nonnegative CHECK (monthly_rent >= 0),
    CONSTRAINT ck_contract_status CHECK (status IN ('DRAFT', 'ACTIVE', 'TERMINATED'))
);

CREATE INDEX ix_contracts_room_status_dates ON rental_contracts(room_id, status, start_date, end_date);
CREATE INDEX ix_contracts_org_status ON rental_contracts(organization_id, status);
