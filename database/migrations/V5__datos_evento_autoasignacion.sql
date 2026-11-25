
-- MS05 - BRIGADISTAS E INCIDENTES
-- Migracion V5: datos del evento que envia MS06 (nombre, simulacro, estado y edificios del alcance).
-- Los usan el panel de gestion de brigadistas (coordinador) y la auto asignacion (brigadista).

BEGIN;

ALTER TABLE configuracion_operacion_evento
    ADD COLUMN IF NOT EXISTS nombre VARCHAR(200),
    ADD COLUMN IF NOT EXISTS simulacro BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS estado_evento VARCHAR(30);

CREATE TABLE IF NOT EXISTS evento_zona (
    evento_id VARCHAR(100) NOT NULL,
    zona_id VARCHAR(100) NOT NULL,
    nombre VARCHAR(200),
    tipo_ubicacion VARCHAR(40) NOT NULL DEFAULT 'EDIFICIO',

    CONSTRAINT pk_evento_zona
        PRIMARY KEY (evento_id, zona_id),

    CONSTRAINT fk_evento_zona_configuracion
        FOREIGN KEY (evento_id)
        REFERENCES configuracion_operacion_evento(evento_id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_asignacion_evento_estado
    ON asignacion_brigadista (evento_id, estado_id);

COMMIT;
