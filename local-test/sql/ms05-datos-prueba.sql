-- Datos de prueba de MS05 para el entorno local. Se puede ejecutar varias veces: reinicia los dos eventos de prueba.
-- Los eventos (nombre, simulacro, estado, edificios) los registra antes cargar-datos.ps1, simulando la llamada de MS06.
BEGIN;

DELETE FROM asignacion_brigadista
 WHERE evento_id IN ('33333333-0000-4000-8000-000000000001', '33333333-0000-4000-8000-000000000002');
DELETE FROM historial_incidente
 WHERE incidente_id IN (SELECT id FROM incidente WHERE evento_id = '33333333-0000-4000-8000-000000000001');
DELETE FROM evidencia_incidente
 WHERE incidente_id IN (SELECT id FROM incidente WHERE evento_id = '33333333-0000-4000-8000-000000000001');
DELETE FROM incidente WHERE evento_id = '33333333-0000-4000-8000-000000000001';

-- Estado de los brigadistas: Ana, Carlos, Laura y Diego activos; Sofía inactiva
INSERT INTO brigadista (id, usuario_id_externo, estado_id)
SELECT gen_random_uuid(), b.id, (SELECT id FROM estado_brigadista WHERE nombre = b.estado)
  FROM (VALUES
        ('22222222-0000-4000-8000-000000000001', 'ACTIVO'),
        ('22222222-0000-4000-8000-000000000002', 'ACTIVO'),
        ('22222222-0000-4000-8000-000000000003', 'ACTIVO'),
        ('22222222-0000-4000-8000-000000000004', 'ACTIVO'),
        ('22222222-0000-4000-8000-000000000005', 'INACTIVO')) AS b(id, estado)
ON CONFLICT (usuario_id_externo) DO UPDATE SET estado_id = EXCLUDED.estado_id;

-- Evento real en ejecución: Ana sola en A, Carlos y Laura en B; Diego activo sin edificio
INSERT INTO asignacion_brigadista (id_asignacion, brigadista_id, evento_id, ubicacion_id, tipo_ubicacion_id, estado_id, fecha_creacion, version)
SELECT gen_random_uuid(), a.brigadista, '33333333-0000-4000-8000-000000000001', a.ubicacion,
       (SELECT id FROM tipo_ubicacion WHERE nombre = 'EDIFICIO'),
       (SELECT id FROM estado_asignacion WHERE nombre = 'VIGENTE'), now(), 0
  FROM (VALUES
        ('22222222-0000-4000-8000-000000000001', 'BLD-A'),
        ('22222222-0000-4000-8000-000000000002', 'BLD-B'),
        ('22222222-0000-4000-8000-000000000003', 'BLD-B')) AS a(brigadista, ubicacion);

-- Simulacro programado: Carlos solo en el Edificio F
INSERT INTO asignacion_brigadista (id_asignacion, brigadista_id, evento_id, ubicacion_id, tipo_ubicacion_id, estado_id, fecha_creacion, version)
VALUES (gen_random_uuid(), '22222222-0000-4000-8000-000000000002', '33333333-0000-4000-8000-000000000002', 'BLD-F',
        (SELECT id FROM tipo_ubicacion WHERE nombre = 'EDIFICIO'),
        (SELECT id FROM estado_asignacion WHERE nombre = 'VIGENTE'), now(), 0);

-- Incidentes del evento real: Ana atiende 1 y resolvió 2; Carlos resolvió 1
INSERT INTO incidente (id, evento_id, nombre_afectado, reportante_id, ubicacion_id, tipo_ubicacion_id, prioridad_id, estado_id,
                       descripcion, fecha_reporte, brigadista_responsable_id, fecha_primera_atencion, fecha_resolucion)
SELECT gen_random_uuid(), '33333333-0000-4000-8000-000000000001', i.afectado, '11111111-0000-4000-8000-000000000001', i.ubicacion,
       (SELECT id FROM tipo_ubicacion WHERE nombre = 'EDIFICIO'),
       (SELECT id FROM prioridad_incidente WHERE nombre = 'URGENCIA_MODERADA'),
       (SELECT id FROM estado_incidente WHERE nombre = i.estado),
       'Incidente de prueba', now() - interval '20 minutes', i.responsable,
       now() - interval '15 minutes',
       CASE WHEN i.estado = 'RESUELTO' THEN now() - interval '5 minutes' END
  FROM (VALUES
        ('Persona con mareo', 'BLD-A', 'EN_ATENCION', '22222222-0000-4000-8000-000000000001'),
        ('Golpe en la rodilla', 'BLD-A', 'RESUELTO', '22222222-0000-4000-8000-000000000001'),
        ('Crisis de ansiedad', 'BLD-A', 'RESUELTO', '22222222-0000-4000-8000-000000000001'),
        ('Corte leve en la mano', 'BLD-B', 'RESUELTO', '22222222-0000-4000-8000-000000000002')) AS i(afectado, ubicacion, estado, responsable);

COMMIT;
