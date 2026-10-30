
-- Tabla de asignaciones de brigadistas de MS05

CREATE TABLE IF NOT EXISTS asignaciones_brigadistas (
    id_asignacion UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Referencia al brigadista registrado en MS05
    brigadista_id UUID NOT NULL,

    -- Identificadores administrados por MS06 y MS02
    evento_id UUID NOT NULL,
    ubicacion_id UUID NOT NULL,

    -- Tipo de ubicación: EDIFICIO, ESPACIO o ZONA_SEGURA
    tipo_ubicacion VARCHAR(20) NOT NULL,

    -- DEFAULT para asignaciones iniciales, MANUAL si el coordinador la modifica
    tipo_asignacion VARCHAR(20) NOT NULL DEFAULT 'DEFAULT',

    -- Estado de la asignación
    estado VARCHAR(20) NOT NULL DEFAULT 'VIGENTE',

    observaciones TEXT,

    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,

    -- Relación con la tabla local de brigadistas
    CONSTRAINT fk_asignacion_brigadista
        FOREIGN KEY (brigadista_id)
        REFERENCES brigadistas(id_brigadista),

    -- Validación de TipoUbicacion
    CONSTRAINT chk_asignacion_tipo_ubicacion
        CHECK (
            tipo_ubicacion IN ('EDIFICIO', 'ESPACIO', 'ZONA_SEGURA')
        ),

    -- Validación de TipoAsignacion
    CONSTRAINT chk_asignacion_tipo
        CHECK (
            tipo_asignacion IN ('DEFAULT', 'MANUAL')
        ),

    -- Validación de EstadoAsignacion
    CONSTRAINT chk_asignacion_estado
        CHECK (
            estado IN ('VIGENTE', 'RETIRADA')
        )
);

-- Evita duplicar una asignación vigente para la misma combinación
-- de brigadista, evento y ubicación.
CREATE UNIQUE INDEX IF NOT EXISTS uk_asignacion_vigente
ON asignaciones_brigadistas (
    brigadista_id,
    evento_id,
    ubicacion_id,
    tipo_ubicacion
)
WHERE estado = 'VIGENTE';
