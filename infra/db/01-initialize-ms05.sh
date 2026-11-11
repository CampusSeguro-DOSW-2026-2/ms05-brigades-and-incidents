#!/bin/bash
set -e

echo "Inicializando esquema de MS05..."

echo "1. Creando tablas principales..."
psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --file /database-init/01-create-tables.sql

echo "2. Insertando catalogos..."
psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --file /database-init/02-insert-catalogs.sql

echo "3. Aplicando migracion V2..."
psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --file /migrations/V2__gestion_atencion_incidentes.sql

echo "4. Aplicando migracion V3..."
psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --file /migrations/V3__eliminacion_logica_incidentes.sql

echo "5. Aplicando migracion V4..."
psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --file /migrations/V4__crear_registro_auditoria.sql

echo "Inicializacion de MS05 completada."
