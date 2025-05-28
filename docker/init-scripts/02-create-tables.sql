-- Setor do estacionamento
CREATE TABLE parking.sectors (
    id SERIAL PRIMARY KEY,
    sector_code VARCHAR(10) NOT NULL UNIQUE,
    base_price DECIMAL(10,2) NOT NULL,
    max_capacity INTEGER NOT NULL,
    open_hour TIME NOT NULL,
    close_hour TIME NOT NULL,
    duration_limit_minutes INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Vagas do estacionamento
CREATE TABLE parking.spots (
    id SERIAL PRIMARY KEY,
    spot_number INTEGER NOT NULL,
    sector_id BIGINT NOT NULL REFERENCES parking.sectors(id),
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    is_occupied BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(sector_id, spot_number)
);

-- Sessões de estacionamento
CREATE TABLE parking.parking_sessions (
    id SERIAL PRIMARY KEY,
    license_plate VARCHAR(20) NOT NULL,
    sector_id BIGINT NOT NULL REFERENCES parking.sectors(id),
    spot_id BIGINT REFERENCES parking.spots(id),
    entry_time TIMESTAMP WITH TIME ZONE NOT NULL,
    parked_time TIMESTAMP WITH TIME ZONE,
    exit_time TIMESTAMP WITH TIME ZONE,
    base_price DECIMAL(10,2) NOT NULL,
    dynamic_price DECIMAL(10,2) NOT NULL,
    calculated_amount DECIMAL(10,2),
    occupancy_rate_at_entry DECIMAL(5,2),
    status VARCHAR(20) NOT NULL DEFAULT 'ENTERED', -- ENTERED, PARKED, CALCULATED, EXITED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Eventos de entrada/saída
CREATE TABLE parking.vehicle_events (
    id SERIAL PRIMARY KEY,
    license_plate VARCHAR(20) NOT NULL,
    event_type VARCHAR(20) NOT NULL, -- ENTRY, PARKED, EXIT
    event_time TIMESTAMP WITH TIME ZONE NOT NULL,
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    session_id BIGINT REFERENCES parking.parking_sessions(id),
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Faturamento diário por setor
CREATE TABLE parking.daily_revenue (
    id SERIAL PRIMARY KEY,
    sector_id BIGINT NOT NULL REFERENCES parking.sectors(id),
    revenue_date DATE NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_sessions INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(sector_id, revenue_date)
);

-- Índices para performance
CREATE INDEX idx_spots_sector ON parking.spots(sector_id);
CREATE INDEX idx_spots_coordinates ON parking.spots(latitude, longitude);
CREATE INDEX idx_spots_occupied ON parking.spots(is_occupied);

CREATE INDEX idx_sessions_plate ON parking.parking_sessions(license_plate);
CREATE INDEX idx_sessions_sector ON parking.parking_sessions(sector_id);
CREATE INDEX idx_sessions_status ON parking.parking_sessions(status);
CREATE INDEX idx_sessions_entry_time ON parking.parking_sessions(entry_time);

CREATE INDEX idx_events_plate ON parking.vehicle_events(license_plate);
CREATE INDEX idx_events_type ON parking.vehicle_events(event_type);
CREATE INDEX idx_events_time ON parking.vehicle_events(event_time);

CREATE INDEX idx_revenue_sector_date ON parking.daily_revenue(sector_id, revenue_date);

-- Comentários nas tabelas
COMMENT ON TABLE parking.sectors IS 'Setores do estacionamento com configurações';
COMMENT ON TABLE parking.spots IS 'Vagas individuais do estacionamento';
COMMENT ON TABLE parking.parking_sessions IS 'Sessões de estacionamento dos veículos';
COMMENT ON TABLE parking.vehicle_events IS 'Log de eventos de veículos';
COMMENT ON TABLE parking.daily_revenue IS 'Faturamento diário agregado por setor';
