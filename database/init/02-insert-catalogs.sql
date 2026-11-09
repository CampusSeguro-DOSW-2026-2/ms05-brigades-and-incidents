
-- MS05 - BRIGADISTAS E INCIDENTES
-- Script 02: Datos iniciales de los catálogos

-- =====================================================
-- 1. ESTADOS DE ASIGNACIÓN
-- =====================================================

INSERT INTO estado_asignacion (nombre, descripcion)
VALUES
    ('VIGENTE', 'La asignación se encuentra activa'),
    ('RETIRADA', 'La asignación fue retirada')
ON CONFLICT (nombre) DO NOTHING;


-- =====================================================
-- 2. TIPOS DE UBICACIÓN
-- =====================================================

INSERT INTO tipo_ubicacion (nombre, descripcion)
VALUES
    ('EDIFICIO', 'Asignación correspondiente a un edificio'),
    ('ZONA_SEGURA', 'Asignación correspondiente a una zona segura'),
    ('PUNTO_ENCUENTRO', 'Asignación correspondiente a un punto de encuentro')
ON CONFLICT (nombre) DO NOTHING;


-- =====================================================
-- 3. ESTADOS DE BRIGADISTA
-- =====================================================

INSERT INTO estado_brigadista (nombre, descripcion)
VALUES
    ('INACTIVO', 'Brigadista registrado que aún no está habilitado'),
    ('ACTIVO', 'Brigadista habilitado para participar')
ON CONFLICT (nombre) DO NOTHING;


-- =====================================================
-- 4. ESTADOS DE INCIDENTE
-- =====================================================

INSERT INTO estado_incidente (nombre, descripcion)
VALUES
    ('REPORTADO', 'Incidente registrado sin brigadistas asignados a la ubicación.'),
    ('ASIGNADO', 'Incidente registrado con brigadistas disponibles para atenderlo.')
ON CONFLICT (nombre) DO NOTHING;


-- =====================================================
-- 5. PRIORIDADES DE INCIDENTE
-- =====================================================

INSERT INTO prioridad_incidente (nombre, descripcion)
VALUES
    ('EMERGENCIA', 'Prioridad alta. Situaciones con riesgo vital.'),
    ('URGENCIA_MODERADA', 'Prioridad media. Situaciones que requieren atención.'),
    ('POCA_URGENCIA', 'Prioridad baja. Situaciones sin riesgo vital inmediato.')
ON CONFLICT (nombre) DO NOTHING;
