-- MS05 - BRIGADISTAS E INCIDENTES
-- Script 01: Creación de tablas iniciales

-- 1. Tabla de estados de las asignaciones
CREATE TABLE IF NOT EXISTS estado_asignacion (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);

-- 2. Tabla de tipos de ubicación
CREATE TABLE IF NOT EXISTS tipo_ubicacion (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);

-- 3. Tabla principal de asignaciones de brigadistas
CREATE TABLE IF NOT EXISTS asignacion_brigadista (
    id_asignacion UUID PRIMARY KEY,

    brigadista_id VARCHAR(100) NOT NULL,
    evento_id VARCHAR(100) NOT NULL,
    ubicacion_id VARCHAR(100) NOT NULL,

    tipo_ubicacion_id BIGINT NOT NULL,
    estado_id BIGINT NOT NULL,

    observaciones VARCHAR(5000),

    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP,

    version BIGINT,

    CONSTRAINT fk_asignacion_tipo_ubicacion
        FOREIGN KEY (tipo_ubicacion_id)
        REFERENCES tipo_ubicacion(id),

    CONSTRAINT fk_asignacion_estado
        FOREIGN KEY (estado_id)
        REFERENCES estado_asignacion(id)
);

-- 4. Índices para facilitar las consultas
CREATE INDEX IF NOT EXISTS idx_asignacion_brigadista
    ON asignacion_brigadista(brigadista_id);

CREATE INDEX IF NOT EXISTS idx_asignacion_evento
    ON asignacion_brigadista(evento_id);

CREATE INDEX IF NOT EXISTS idx_asignacion_ubicacion
    ON asignacion_brigadista(ubicacion_id);

CREATE INDEX IF NOT EXISTS idx_asignacion_estado
    ON asignacion_brigadista(estado_id);

