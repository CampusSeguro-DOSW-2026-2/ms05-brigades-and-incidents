
-- MS05 - BRIGADISTAS E INCIDENTES
-- Migracion V4: Registro y consulta de auditoria.

BEGIN;

-- =====================================================
-- 1. CATALOGO DE TIPOS DE REGISTRO
-- =====================================================

CREATE TABLE tipo_registro_auditoria (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(150)
);

INSERT INTO tipo_registro_auditoria (nombre, descripcion)
VALUES
    ('ASIGNACION', 'Acciones relacionadas con asignaciones de brigadistas'),
    ('SOLICITUD', 'Acciones relacionadas con solicitudes'),
    ('INCIDENTE', 'Acciones relacionadas con incidentes'),
    ('EVIDENCIA', 'Acciones relacionadas con evidencias de incidentes');

-- =====================================================
-- 2. TABLA DE REGISTROS DE AUDITORIA
-- =====================================================

CREATE TABLE registro_auditoria (
    id UUID PRIMARY KEY,
    tipo_registro_id BIGINT NOT NULL,
    usuario_origen VARCHAR(100) NOT NULL,
    accion VARCHAR(100) NOT NULL,
    recurso_tipo VARCHAR(50) NOT NULL,
    recurso_id VARCHAR(100) NOT NULL,
    fecha_hora_registro TIMESTAMP WITH TIME ZONE NOT NULL,
    evento_id VARCHAR(100),
    origen VARCHAR(100) NOT NULL,
    resultado VARCHAR(50) NOT NULL,
    detalle VARCHAR(1000),

    CONSTRAINT fk_auditoria_tipo_registro
        FOREIGN KEY (tipo_registro_id)
        REFERENCES tipo_registro_auditoria(id)
);

-- =====================================================
-- 3. INDICES
-- =====================================================

CREATE INDEX idx_auditoria_evento
    ON registro_auditoria(evento_id);

CREATE INDEX idx_auditoria_recurso
    ON registro_auditoria(recurso_tipo, recurso_id);

CREATE INDEX idx_auditoria_fecha
    ON registro_auditoria(fecha_hora_registro);

CREATE INDEX idx_auditoria_tipo
    ON registro_auditoria(tipo_registro_id);

COMMIT;
