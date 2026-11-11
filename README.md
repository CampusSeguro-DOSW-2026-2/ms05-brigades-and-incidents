# ms05-brigadistas-and-incidents

MS05 - Brigadistas e incidentes: activación y asignación de brigadistas, responsabilidades, incidentes y su
atención, y la integración con MS06 (BEI). Java 21, Spring Boot 4, PostgreSQL propia. Puerto interno `8085`.

## Rutas y contrato

Detrás del API Gateway, con estos prefijos:

| Prefijo | Qué contiene |
|---|---|
| `/api/v1/brigadistas/**` | Activación y búsqueda de brigadistas |
| `/api/v1/asignaciones/**` | Asignaciones de brigadistas a zonas |
| `/api/v1/responsabilidades/**` | Panel de responsabilidades |
| `/api/v1/incidentes/eventos/{eventoId}/**` | Incidentes de un evento, su atención y sus evidencias |
| `/api/v1/bei/eventos/{eventoId}/**` | Rutas internas de MS06 (estado, brigadistas, indicadores) |
| `/api/v1/bei/auditoria/**` | Registro de auditoría de MS05 |

* **Identidad:** el Gateway envía `X-User-Id` y `X-User-Role` (roles separados por coma, p. ej.
  `BRIGADISTA,ESTUDIANTE`). Sin ellos, la respuesta es `401`.
* **Rutas internas de MS06:** se autorizan por `X-Service-Name: ms06`, que el Gateway deriva de la clave de servicio
  de MS06. No existe un rol de servicio en `X-User-Role`.
* **Errores:** todos salen con el `ErrorResponse` de los Estándares: `status`, `codigo`, `mensaje`, `errores` y
  `correlation_id`.
* **Llamadas a MS01 y MS02:** siempre a través del Gateway (`MS05_GATEWAY_URL`), con la clave `GW_SERVICE_KEY_MS05`.
  A MS02 se le propagan `X-User-Id`, `X-User-Role` y `X-Correlation-ID`.
* **Health:** `GET /actuator/health` responde `{"status":"UP"}`. Solo se consulta dentro de la VM; el Gateway no lo
  enruta.

## Pruebas

```bash
mvn verify
```

Ejecuta las pruebas, exige 80 % de cobertura de líneas (JaCoCo) y SpotBugs sin hallazgos. El pipeline hace lo mismo.

## CI/CD

| Workflow | Cuándo | Qué hace |
|---|---|---|
| `ci-quality.yml` | Cada push y PR a `develop` o `main` | Pruebas, cobertura y SpotBugs. Desde `develop` y `main` publica la imagen en `ghcr.io/campusseguro-dosw-2026-2/ms05-brigades-and-incidents-api` con `:develop` o `:latest` y `:sha-xxxxxxx`. |
| `ci-quality.yml` → `deploy.yml` | Push a `develop` | Despliega esa imagen en el entorno de **prueba** (`:8443`). |
| `deploy-prod.yml` | A mano, desde `main` | Despliega en **producción** (`:443`) una imagen `sha-xxxxxxx` que ya pasó por prueba, sin reconstruirla. Otro tag anterior es el rollback. |

## Despliegue en la nube (prueba y producción)

MS05 corre en su propia VM, detrás del API Gateway. La guía completa es
[Conexión al API Gateway](https://github.com/CampusSeguro-DOSW-2026-2/documentation/wiki/Conexi%C3%B3n-al-API-Gateway):
* El firewall solo acepta la IP del GW en `443` (producción) y `8443` (prueba), y la IP de quien administra en `22`.
* Un Caddy de borde exige el certificado de cliente del GW (mTLS).
* Prueba y producción comparten la VM, cada una con su stack (MS05 + PostgreSQL + evidencias) y sus secretos.

Los pasos 1 a 3 se hacen una sola vez. Los comandos marcados *(VM)* van en la VM por SSH.

### 1. Borde Caddy (mTLS) *(VM)*
Con la carpeta `certs/` que entrega el equipo del GW (`prod/` y `test/`, cada una con `servidor.crt`,
`servidor.key` y `ca.pem`; nunca va al repositorio):
```bash
curl -fsSL https://get.docker.com | sudo sh && sudo usermod -aG docker "$USER"   # una vez; salga y vuelva a entrar
mkdir -p ~/campusseguro/edge && cd ~/campusseguro/edge
curl -fsSLO https://raw.githubusercontent.com/CampusSeguro-DOSW-2026-2/api-gateway/develop/infra/caddy/Caddyfile.microservicio
curl -fsSLO https://raw.githubusercontent.com/CampusSeguro-DOSW-2026-2/api-gateway/develop/infra/caddy/docker-compose.microservicio.yml
printf 'UPSTREAM_PROD=ms05-prod:8085\nUPSTREAM_TEST=ms05-test:8085\n' > .env
chmod 600 certs/*/servidor.key
docker compose -f docker-compose.microservicio.yml up -d
```

### 2. Runner self-hosted
En GitHub: **Settings › Actions › Runners › New self-hosted runner** (Linux, x64), con la etiqueta `ms05`, instalado
como servicio *(VM)*: `./config.sh --url https://github.com/CampusSeguro-DOSW-2026-2/ms05-brigades-and-incidents --token <TOKEN> --labels ms05 --unattended`
y `sudo ./svc.sh install "$USER" && sudo ./svc.sh start`.

### 3. Environments de GitHub
`ms05-test` (**Deployment branches**: solo `develop`) y `ms05-prod` (**Required reviewers** y solo `main`):

| En `ms05-test` y `ms05-prod` | Tipo | Valor |
|---|---|---|
| `GATEWAY_URL` | Variable | Prueba: `https://campusseguro-gw.northcentralus.cloudapp.azure.com:8443`. Producción: sin `:8443`. |
| `MS05_EXTRA_ENV` | Variable (opcional) | Líneas `CLAVE=valor` extra para MS05. |
| `DEPLOY_DIR` | Variable (opcional) | `~/campusseguro/ms05` por defecto. |
| `DB_PASSWORD` | Secreto | `openssl rand -base64 24`, distinta por entorno. Solo se aplica al crear la BD. |
| `GATEWAY_SERVICE_KEY` | Secreto | La que entregó el equipo del GW para ese entorno (`GW_SERVICE_KEY_MS05` del GW). |

### 4. Desplegar
1. **Prueba:** cada merge a `develop` publica la imagen y la despliega (job **desplegar-test**).
2. **Producción:** después de mezclar en `main`, **Actions › Desplegar a producción › Run workflow**, rama `main`,
   con el tag que ya funciona en prueba, y apruebe el despliegue.

`infra/deploy.sh` verifica cada despliegue dentro de la VM: health 200 por la red `campus-edge` y el borde rechaza
conexiones sin el certificado del GW. El esquema de la BD lo crea `infra/db/01-initialize-ms05.sh` (scripts de
`database/init` y `database/migrations`) solo la primera vez. Operación *(VM)*: `cd ~/campusseguro/ms05/test &&
docker compose ps && docker compose logs --tail=100 ms05` (nunca `down -v`: borra la BD y las evidencias).
