-- Usuarios de prueba para el entorno local (solo desarrollo).
-- Contraseña de TODOS los usuarios: CampusSeguro2026*
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO usuarios (id, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, correo, password_hash)
VALUES
    ('11111111-0000-4000-8000-000000000001', 'Camila', NULL, 'Coordinadora', NULL, 'coordinador@escuelaing.edu.co', crypt('CampusSeguro2026*', gen_salt('bf', 10))),
    ('11111111-0000-4000-8000-000000000002', 'Oscar', NULL, 'Organizador', NULL, 'organizador@escuelaing.edu.co', crypt('CampusSeguro2026*', gen_salt('bf', 10))),
    ('22222222-0000-4000-8000-000000000001', 'Ana', 'María', 'Díaz', NULL, 'brigadista1@escuelaing.edu.co', crypt('CampusSeguro2026*', gen_salt('bf', 10))),
    ('22222222-0000-4000-8000-000000000002', 'Carlos', NULL, 'Pardo', NULL, 'brigadista2@escuelaing.edu.co', crypt('CampusSeguro2026*', gen_salt('bf', 10))),
    ('22222222-0000-4000-8000-000000000003', 'Laura', NULL, 'Gómez', NULL, 'brigadista3@escuelaing.edu.co', crypt('CampusSeguro2026*', gen_salt('bf', 10))),
    ('22222222-0000-4000-8000-000000000004', 'Diego', NULL, 'Ruiz', NULL, 'brigadista4@escuelaing.edu.co', crypt('CampusSeguro2026*', gen_salt('bf', 10))),
    ('22222222-0000-4000-8000-000000000005', 'Sofía', NULL, 'Mora', NULL, 'brigadista5@escuelaing.edu.co', crypt('CampusSeguro2026*', gen_salt('bf', 10)))
ON CONFLICT DO NOTHING;

INSERT INTO usuario_rol (usuario_id, rol)
VALUES
    ('11111111-0000-4000-8000-000000000001', 'COORDINADOR'),
    ('11111111-0000-4000-8000-000000000002', 'ORGANIZADOR'),
    ('22222222-0000-4000-8000-000000000001', 'BRIGADISTA'),
    ('22222222-0000-4000-8000-000000000002', 'BRIGADISTA'),
    ('22222222-0000-4000-8000-000000000003', 'BRIGADISTA'),
    ('22222222-0000-4000-8000-000000000004', 'BRIGADISTA'),
    ('22222222-0000-4000-8000-000000000005', 'BRIGADISTA')
ON CONFLICT DO NOTHING;

-- SOLO ENTORNO LOCAL: la matriz oficial de MS01 no da permisos de MS05 al ORGANIZADOR, pero MS05 (README y
-- PermisosPrueba) asume que el organizador gestiona brigadistas. Se agregan aquí para poder probar ese perfil.
UPDATE roles
   SET permisos = ARRAY(SELECT DISTINCT unnest(permisos || ARRAY['ms05:brigadier:assign', 'ms05:incident:read', 'ms05:incident:report']) ORDER BY 1)
 WHERE nombre = 'ORGANIZADOR';
