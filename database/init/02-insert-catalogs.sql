-- MS05 - BRIGADISTAS E INCIDENTES
-- Script 02: Datos iniciales de los catálogos

-- Estados de las asignaciones
INSERT INTO estado_asignacion (nombre, descripcion)
VALUES
    ('VIGENTE', 'Asignación activa del brigadista'),
    ('RETIRADA', 'Asignación retirada, conservada como historial')
ON CONFLICT (nombre) DO NOTHING;


-- Tipos de ubicación
INSERT INTO tipo_ubicacion (nombre, descripcion)
VALUES
    ('EDIFICIO', 'Edificio perteneciente al campus'),
    ('ZONA_SEGURA', 'Zona segura del campus'),
    ('PUNTO_ENCUENTRO', 'Punto de encuentro establecido')
ON CONFLICT (nombre) DO NOTHING;

