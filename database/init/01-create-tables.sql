
-- MS05 - BRIGADISTAS E INCIDENTES
-- Script 01: Creación de tablas

-- =====================================================
-- 1. CATÁLOGOS
-- =====================================================

CREATE TABLE IF NOT EXISTS estado_asignacion (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(150)
);

CREATE TABLE IF NOT EXISTS tipo_ubicacion (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(40) NOT NULL UNIQUE,
    descripcion VARCHAR(150)
);

CREATE TABLE IF NOT EXISTS estado_brigadista (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(150)
);

CREATE TABLE IF NOT EXISTS estado_incidente (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS prioridad_incidente (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);


-- =====================================================
-- 2. BRIGADISTAS
-- =====================================================

CREATE TABLE IF NOT EXISTS brigadista (
    id UUID PRIMARY KEY,
    usuario_id_externo VARCHAR(255) NOT NULL UNIQUE,
    estado_id BIGINT NOT NULL,

    CONSTRAINT fk_brigadista_estado
        FOREIGN KEY (estado_id)
        REFERENCES estado_brigadista(id)
);


-- =====================================================
-- 3. CONFIGURACIÓN DE OPERACIÓN POR EVENTO
-- =====================================================

CREATE TABLE IF NOT EXISTS configuracion_operacion_evento (
    evento_id VARCHAR(100) PRIMARY KEY,
    autoasignacion_habilitada BOOLEAN NOT NULL,
    panel_habilitado BOOLEAN NOT NULL,
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL
);


-- =====================================================
-- 4. ASIGNACIONES DE BRIGADISTAS
-- =====================================================

CREATE TABLE IF NOT EXISTS asignacion_brigadista (
    id_asignacion UUID PRIMARY KEY,

    brigadista_id VARCHAR(100) NOT NULL,
    evento_id VARCHAR(100) NOT NULL,
    ubicacion_id VARCHAR(100) NOT NULL,

    tipo_ubicacion_id BIGINT NOT NULL,
    estado_id BIGINT NOT NULL,

    observaciones TEXT,

    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_asignacion_tipo_ubicacion
        FOREIGN KEY (tipo_ubicacion_id)
        REFERENCES tipo_ubicacion(id),

    CONSTRAINT fk_asignacion_estado
        FOREIGN KEY (estado_id)
        REFERENCES estado_asignacion(id)
);


-- =====================================================
-- 5. INCIDENTES
-- =====================================================

CREATE TABLE IF NOT EXISTS incidente (
    id UUID PRIMARY KEY,

    evento_id VARCHAR(100) NOT NULL,
    nombre_afectado VARCHAR(150) NOT NULL,
    reportante_id VARCHAR(100) NOT NULL,
    ubicacion_id VARCHAR(100) NOT NULL,

    tipo_ubicacion_id BIGINT NOT NULL,
    prioridad_id BIGINT NOT NULL,
    estado_id BIGINT NOT NULL,

    descripcion VARCHAR(1000),

    fecha_reporte TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_incidente_tipo_ubicacion
        FOREIGN KEY (tipo_ubicacion_id)
        REFERENCES tipo_ubicacion(id),

    CONSTRAINT fk_incidente_prioridad
        FOREIGN KEY (prioridad_id)
        REFERENCES prioridad_incidente(id),

    CONSTRAINT fk_incidente_estado
        FOREIGN KEY (estado_id)
        REFERENCES estado_incidente(id)
);


-- =====================================================
-- 6. EVIDENCIAS DE INCIDENTES
-- =====================================================

CREATE TABLE IF NOT EXISTS evidencia_incidente (
    id UUID PRIMARY KEY,

    nombre_archivo VARCHAR(255) NOT NULL,
    tipo_contenido VARCHAR(100) NOT NULL,
    referencia_archivo VARCHAR(500) NOT NULL,

    incidente_id UUID NOT NULL,

    fecha_carga TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_evidencia_incidente
        FOREIGN KEY (incidente_id)
        REFERENCES incidente(id)
);


-- =====================================================
-- 7. ÍNDICES
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_asignacion_brigadista
    ON asignacion_brigadista(brigadista_id);

CREATE INDEX IF NOT EXISTS idx_asignacion_evento
    ON asignacion_brigadista(evento_id);

CREATE INDEX IF NOT EXISTS idx_asignacion_ubicacion
    ON asignacion_brigadista(ubicacion_id);

CREATE INDEX IF NOT EXISTS idx_asignacion_estado
    ON asignacion_brigadista(estado_id);

CREATE INDEX IF NOT EXISTS idx_brigadista_estado
    ON brigadista(estado_id);

CREATE INDEX IF NOT EXISTS idx_incidente_evento
    ON incidente(evento_id);

CREATE INDEX IF NOT EXISTS idx_incidente_reportante
    ON incidente(reportante_id);

CREATE INDEX IF NOT EXISTS idx_incidente_ubicacion
    ON incidente(ubicacion_id);

CREATE INDEX IF NOT EXISTS idx_incidente_estado
    ON incidente(estado_id);

CREATE INDEX IF NOT EXISTS idx_incidente_prioridad
    ON incidente(prioridad_id);

CREATE INDEX IF NOT EXISTS idx_evidencia_incidente
    ON evidencia_incidente(incidente_id);
