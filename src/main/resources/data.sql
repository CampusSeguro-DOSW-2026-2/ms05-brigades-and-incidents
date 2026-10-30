INSERT INTO configuracion_asignaciones (
    id_configuracion,
    tipo_ubicacion,
    cantidad_brigadistas
)
VALUES
    (gen_random_uuid(), 'EDIFICIO', 4),
    (gen_random_uuid(), 'ESPACIO', 3),
    (gen_random_uuid(), 'ZONA_SEGURA', 3)
ON CONFLICT (tipo_ubicacion) DO NOTHING;