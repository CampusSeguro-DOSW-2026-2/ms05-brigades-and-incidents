
-- Tabla de brigadistas de MS05

CREATE TABLE IF NOT EXISTS brigadistas (
    id_brigadista UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Identificador del usuario en MS01
    usuario_id UUID NOT NULL UNIQUE,

    estado_operativo VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    observaciones TEXT,

    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,

    CONSTRAINT chk_brigadista_estado
        CHECK (estado_operativo IN ('ACTIVO', 'INACTIVO'))
);
