CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE organization_memberships (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    user_id UUID NOT NULL REFERENCES users(id),
    role VARCHAR(24) NOT NULL,
    CONSTRAINT ck_membership_role CHECK (role IN ('OWNER', 'MANAGER', 'STAFF')),
    CONSTRAINT uk_membership_org_user UNIQUE (organization_id, user_id)
);

CREATE INDEX ix_memberships_user ON organization_memberships(user_id);

CREATE TABLE properties (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(120) NOT NULL,
    address VARCHAR(300) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_property_org_id UNIQUE (organization_id, id),
    CONSTRAINT uk_property_org_name_address UNIQUE (organization_id, name, address)
);

CREATE INDEX ix_properties_organization ON properties(organization_id);

CREATE TABLE rooms (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    property_id UUID NOT NULL,
    room_code VARCHAR(40) NOT NULL,
    area_square_meters NUMERIC(8, 2) NOT NULL,
    capacity SMALLINT NOT NULL,
    monthly_rent NUMERIC(19, 0) NOT NULL,
    deposit_amount NUMERIC(19, 0) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rooms_property_org FOREIGN KEY (organization_id, property_id)
        REFERENCES properties(organization_id, id),
    CONSTRAINT uk_room_property_code UNIQUE (property_id, room_code),
    CONSTRAINT uk_room_org_id UNIQUE (organization_id, id),
    CONSTRAINT ck_room_area_positive CHECK (area_square_meters > 0),
    CONSTRAINT ck_room_capacity_positive CHECK (capacity > 0),
    CONSTRAINT ck_room_rent_nonnegative CHECK (monthly_rent >= 0),
    CONSTRAINT ck_room_deposit_nonnegative CHECK (deposit_amount >= 0),
    CONSTRAINT ck_room_status CHECK (status IN ('AVAILABLE', 'RESERVED', 'OCCUPIED', 'MAINTENANCE', 'INACTIVE'))
);

CREATE INDEX ix_rooms_organization_property ON rooms(organization_id, property_id);
