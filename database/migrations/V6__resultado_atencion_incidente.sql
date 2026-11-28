-- MS05 - BRIGADISTAS E INCIDENTES
-- Migracion V6: resultado de la atencion registrado por el brigadista responsable.

BEGIN;

ALTER TABLE incidente
    ADD COLUMN IF NOT EXISTS resultado_atencion VARCHAR(30);

COMMIT;
