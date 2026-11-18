-- V1: baseline del esquema PoolHawk.
-- Usa IF NOT EXISTS para ser idempotente.

CREATE TABLE IF NOT EXISTS partidas (
    id VARCHAR(255) PRIMARY KEY,
    modalidad VARCHAR(50),
    fase VARCHAR(50),
    fecha_creacion TIMESTAMP(6) WITH TIME ZONE,
    ganador_id VARCHAR(255),
    formato VARCHAR(50),
    estado_partida_json TEXT,
    estado_mesa_json TEXT,
    version BIGINT
);

CREATE TABLE IF NOT EXISTS jugadores (
    id VARCHAR(255) PRIMARY KEY,
    nombre VARCHAR(255),
    equipo VARCHAR(1),
    partida_id VARCHAR(255) REFERENCES partidas(id),
    orden INTEGER
);

CREATE TABLE IF NOT EXISTS tiros (
    id VARCHAR(255) PRIMARY KEY, -- H-06: ID interno UUID
    event_id VARCHAR(255) NOT NULL, -- H-06: ID de idempotencia
    partida_id VARCHAR(255) NOT NULL REFERENCES partidas(id),
    jugador_id VARCHAR(255) NOT NULL,
    primera_bola_tocada INTEGER,
    bolas_embolsadas_json TEXT,
    troneras_json TEXT,
    timestamp_tiro TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    duracion_segundos BIGINT NOT NULL,
    resultado_json TEXT,
    orden INTEGER,
    CONSTRAINT uk_tiros_partida_event UNIQUE (partida_id, event_id)
);

CREATE INDEX IF NOT EXISTS idx_tiros_partida_id ON tiros(partida_id);
CREATE INDEX IF NOT EXISTS idx_jugadores_partida_id ON jugadores(partida_id);