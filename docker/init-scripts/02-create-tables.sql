CREATE TABLE parking.sector (
    id SERIAL PRIMARY KEY,
    sector_code VARCHAR(2) NOT NULL UNIQUE,
    base_price DECIMAL(10,2) NOT NULL,
    max_capacity INTEGER NOT NULL,
    open_hour TIME NOT NULL,
    close_hour TIME NOT NULL,
    duration_limit_minutes INTEGER NOT NULL,
    currency_code VARCHAR(3),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE parking.spot (
    id SERIAL PRIMARY KEY,
    sector_id BIGINT NOT NULL REFERENCES parking.sector(id),
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE parking.vehicle_event (
    id SERIAL PRIMARY KEY,
    license_plate VARCHAR(20) NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    spot_id BIGINT REFERENCES parking.spot(id),
    sector_id BIGINT REFERENCES parking.sector(id),
    sector_base_price DECIMAL(10,2),
    dynamic_rate DECIMAL(5, 2),
    entry_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
