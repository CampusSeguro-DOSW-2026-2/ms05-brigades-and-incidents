
-- MS05 - BRIGADISTAS E INCIDENTES
-- Migracion V3: Eliminacion logica de incidentes.

BEGIN;

ALTER TABLE incidente
    ADD COLUMN eliminado BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_incidente_eliminado
    ON incidente(eliminado);

COMMIT;
