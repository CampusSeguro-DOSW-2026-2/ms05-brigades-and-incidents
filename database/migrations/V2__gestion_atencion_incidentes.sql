-- MS05 - BRIGADISTAS E INCIDENTES
-- Migracion V2: Gestion de atencion y ciclo de vida de incidentes.

BEGIN;

-- =====================================================
-- 1. COMPLETAR CATALOGO DE ESTADOS DE INCIDENTE
-- =====================================================

INSERT INTO estado_incidente (nombre, descripcion)
VALUES
    ('EN_ATENCION', 'Un brigadista responsable está atendiendo el incidente.'),
    ('RESUELTO', 'El incidente fue atendido y resuelto por el brigadista responsable.'),
    ('CERRADO', 'El coordinador cerró el incidente.')
ON CONFLICT (nombre) DO NOTHING;


-- =====================================================
-- 2. AGREGAR DATOS DE ATENCION A INCIDENTE
-- =====================================================

ALTER TABLE incidente
    ADD COLUMN brigadista_responsable_id VARCHAR(100),
    ADD COLUMN fecha_primera_atencion TIMESTAMP WITH TIME ZONE,
    ADD COLUMN fecha_resolucion TIMESTAMP WITH TIME ZONE,
    ADD COLUMN fecha_cierre TIMESTAMP WITH TIME ZONE,
    ADD COLUMN observaciones_resolucion VARCHAR(1000);


-- =====================================================
-- 3. HISTORIAL DE TRANSICIONES DE INCIDENTES
-- =====================================================

CREATE TABLE historial_incidente (
    id UUID PRIMARY KEY,
    incidente_id UUID NOT NULL,
    estado_anterior_id BIGINT,
    estado_nuevo_id BIGINT NOT NULL,
    responsable_anterior_id VARCHAR(100),
    responsable_nuevo_id VARCHAR(100),
    actor_id VARCHAR(100) NOT NULL,
    observaciones VARCHAR(1000),
    fecha_cambio TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_historial_incidente
        FOREIGN KEY (incidente_id)
        REFERENCES incidente(id),

    CONSTRAINT fk_historial_estado_anterior
        FOREIGN KEY (estado_anterior_id)
        REFERENCES estado_incidente(id),

    CONSTRAINT fk_historial_estado_nuevo
        FOREIGN KEY (estado_nuevo_id)
        REFERENCES estado_incidente(id)
);


-- =====================================================
-- 4. INDICES
-- =====================================================

CREATE INDEX idx_incidente_responsable
    ON incidente(brigadista_responsable_id);

CREATE INDEX idx_historial_incidente
    ON historial_incidente(incidente_id);

CREATE INDEX idx_historial_fecha
    ON historial_incidente(fecha_cambio);

COMMIT;
