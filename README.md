# MS05 – Brigadistas e Incidentes

MS05 es el microservicio de CampusSeguro encargado de gestionar los brigadistas y su participación en los eventos, así como de administrar el registro, asignación y atención de incidentes.

Permite buscar y activar brigadistas, asignarlos a edificios, zonas seguras y puntos de encuentro, habilitar la autoasignación y gestionar sus responsabilidades durante los eventos. También permite registrar incidentes, asignar responsables, hacer seguimiento a su atención y conservar un historial de los cambios realizados.

MS05 trabaja de manera integrada con MS06 (Eventos), MS02 (Campus y Seguridad), MS03 (Alertas) y MS08 (Analítica y Auditoría). Además, mantiene su propia base de datos PostgreSQL y envía los hechos auditables a MS08 para su centralización.

**Información general:**
- **Lenguaje:** Java 21.
- **Framework:** Spring Boot 4.
- **Base de datos:** PostgreSQL propia.
- **Puerto interno:** `8085`.

## 1. Funcionalidades principales

MS05 ofrece las siguientes funcionalidades:

- **Gestión de brigadistas:** permite buscar brigadistas y cambiar su estado entre activo e inactivo.
- **Asignación de brigadistas:** permite asignarlos a edificios, zonas seguras y puntos de encuentro según el evento. También ofrece la opción de autoasignación para los brigadistas y un panel de gestión para los coordinadores.
- **Gestión por evento:** permite administrar las asignaciones en eventos que se encuentren en estado `PROGRAMADO` o `EN_EJECUCION`.
- **Registro de incidentes:** permite crear y editar incidentes, incluidos aquellos que se generan a partir de una solicitud de asistencia realizada desde el check-in.
- **Atención de incidentes:** permite asignar o reasignar responsables, iniciar la atención, resolver y cerrar incidentes. Cuando se asigna un responsable, se puede enviar una notificación por correo mediante MS03.
- **Seguimiento de incidentes:** permite consultar incidentes de forma paginada, aplicar filtros y revisar el historial cronológico de sus cambios de estado.
- **Gestión de evidencias:** permite cargar y consultar archivos relacionados con los incidentes, en formatos PNG, PDF y MP4.
- **Configuración operativa de eventos:** almacena la configuración de cada evento a partir de la información recibida de MS06. Cuando es necesario, consulta MS02 para recuperar los nombres de los edificios y las ubicaciones.
- **Auditoría:** conserva los registros de las acciones importantes realizadas en MS05 y los reenvía a MS08 de manera tolerante a fallos para su centralización y posterior análisis.

## 2. Tecnologías utilizadas

| Elemento | Tecnología o herramienta | Descripción |
|---|---|---|
| Lenguaje | Java 21 | Lenguaje utilizado para desarrollar el microservicio. |
| Framework | Spring Boot 4 | Incluye Spring Web MVC, Spring Security, Bean Validation y Actuator. |
| Dependencias | Maven | Administración de dependencias y construcción del proyecto mediante `./mvnw`. |
| Persistencia | Spring Data JPA y PostgreSQL 17 | Gestión de la información en la base de datos propia de MS05. |
| Esquema de base de datos | Scripts SQL y migraciones | El esquema se crea mediante los archivos de `database/` y se valida al iniciar con `ddl-auto=validate`. |
| Evidencias | Sistema de archivos | Almacena los archivos en el directorio configurado mediante `EVIDENCIAS_DIRECTORIO`. Por defecto, utiliza `./uploads/evidencias`. |
| Calidad | JaCoCo y SpotBugs | JaCoCo verifica una cobertura mínima del 80 % de líneas y SpotBugs analiza posibles problemas en el código. |
| Contenedores | Docker y Docker Compose | Construcción y ejecución del servicio mediante contenedores. La imagen utiliza una construcción multi-etapa con Eclipse Temurin 21. |
| Puerto | `8085` | Puerto interno configurado mediante `SERVER_PORT`. |

## 3. Rutas y contrato de la API

MS05 expone sus funcionalidades mediante una API REST. Las solicitudes externas pasan por el API Gateway, que se encarga de gestionar la entrada al microservicio y la identidad del usuario.

Los principales prefijos de las rutas son:

| Prefijo | Funcionalidad |
|---|---|
| `/api/v1/brigadistas/**` | Búsqueda y activación de brigadistas, panel de gestión de eventos y autoasignación. Incluye `/eventos`, `/eventos/{eventoId}/gestion` y `PUT /eventos/{eventoId}/brigadistas/{brigadistaId}/asignacion`. Para la autoasignación incluye `GET /autoasignacion`, `PUT` y `DELETE /autoasignacion/eventos/{eventoId}`. |
| `/api/v1/asignaciones/**` | Gestión de asignaciones de brigadistas a zonas y recepción de asignaciones predeterminadas provenientes de MS06. |
| `/api/v1/responsabilidades/**` | Consulta del panel de responsabilidades propias y listado general. |
| `/api/v1/incidentes/eventos/{eventoId}/**` | Registro, consulta y atención de incidentes asociados a un evento, incluyendo historial y evidencias. |
| `/api/v1/bei/eventos/{eventoId}/**` | Rutas internas utilizadas por MS06 para consultar información del evento, brigadistas e indicadores. |
| `/api/v1/bei/auditoria/**` | Consulta del registro de auditoría local de MS05. |

### 3.1. Identidad y autorización

La autenticación y autorización de las solicitudes se gestionan a través del API Gateway.

El Gateway envía a MS05 las siguientes cabeceras:

- `X-User-Id`: identificador del usuario que realiza la solicitud.
- `X-User-Permission`: permisos efectivos del usuario, separados por comas. Por ejemplo: `ms05:incident:read,ms05:panel:read_own`.
- `X-User-Role`: rol del usuario. Se utiliza como información complementaria y se propaga a MS02 y MS03 cuando corresponde.

MS05 utiliza exclusivamente los permisos recibidos en `X-User-Permission` para autorizar las operaciones. El rol recibido en `X-User-Role` no sustituye la validación de permisos.

La separación de permisos entre eventos reales y simulacros se realiza mediante permisos específicos, identificados con los sufijos `_to_real` y `_to_drill`.

Las respuestas de autorización son:

- **401 Unauthorized:** cuando no se recibe una identidad válida o faltan los datos de autenticación necesarios.
- **403 Forbidden:** cuando el usuario está identificado, pero no cuenta con el permiso requerido para realizar la operación.

### 3.2. Permisos de MS05

Los permisos controlan qué operaciones puede realizar cada usuario dentro del microservicio.

| Permiso | Funcionalidad autorizada |
|---|---|
| `ms05:brigadier:assign_to_drill` | Gestión, búsqueda, cambio de estado, asignaciones y panel de brigadistas en simulacros. |
| `ms05:brigadier:assign_to_real` | Gestión, búsqueda, cambio de estado, asignaciones y panel de brigadistas en eventos reales. |
| `ms05:panel:read_own` | Autoasignación, consulta del estado propio y panel de responsabilidades propias. |
| `ms05:assistance:assign_to_drill` | Asignación de responsables, cierre y eliminación lógica de incidentes en simulacros. |
| `ms05:assistance:assign_to_real` | Asignación de responsables, cierre y eliminación lógica de incidentes en eventos reales. |
| `ms05:assistance:update_status` | Inicio de atención y resolución de incidentes. |
| `ms05:incident:report` | Registro de incidentes y carga de evidencias. |
| `ms05:incident:manage_own` | Consulta de los incidentes propios. |
| `ms05:incident:read` | Consulta de incidentes, historial y evidencias. |
| `ms05:incident:update` | Edición de incidentes. |
| `ms08:audit_drill:read` | Consulta de auditoría correspondiente a simulacros. |
| `ms08:audit_real_event:read` | Consulta de auditoría correspondiente a eventos reales. |

### 3.3. Comunicación interna con MS06

Las rutas internas destinadas a MS06 se autorizan mediante la cabecera `X-Service-Name: ms06`.

Esta cabecera es incorporada por el API Gateway a partir de la clave de servicio de MS06. No corresponde a un permiso de usuario, sino a la identificación del servicio que realiza la solicitud.

### 3.4. Formato de errores

MS05 utiliza el formato `ErrorResponse` definido en los Estándares del proyecto, específicamente en la sección 7.1.

Las respuestas de error contienen los siguientes campos:

| Campo | Descripción |
|---|---|
| `status` | Código HTTP de la respuesta. |
| `codigo` | Código que identifica el tipo de error. |
| `mensaje` | Descripción del error. |
| `errores` | Detalle de los errores de validación, cuando corresponde. |
| `correlation_id` | Identificador que permite rastrear la solicitud entre los microservicios. |

### 3.5. Integración con otros microservicios

Las comunicaciones de MS05 con MS01, MS02, MS03 y MS08 se realizan a través del API Gateway, utilizando la URL configurada en `MS05_GATEWAY_URL` y la clave de servicio `GW_SERVICE_KEY_MS05`.

- **MS01 – Identidad:** proporciona información de los usuarios y brigadistas mediante sus identificadores.
- **MS02 – Campus y Seguridad:** permite consultar información de edificios y ubicaciones. Cuando corresponde, MS05 propaga las cabeceras de identidad y el identificador de correlación.
- **MS03 – Alertas:** permite enviar notificaciones relacionadas con la asignación o reasignación de responsables de incidentes. También recibe las cabeceras de identidad y correlación cuando corresponde.
- **MS06 – Eventos:** proporciona la información operativa de los eventos, sus estados, brigadistas y datos necesarios para la gestión.
- **MS08 – Analítica y Auditoría:** recibe los hechos auditables generados por MS05 mediante `POST /api/v1/audit/events`. Se propaga la cabecera `X-Correlation-ID` y el envío se realiza de manera tolerante a fallos, para evitar que una indisponibilidad de MS08 interrumpa la operación local de MS05.

### 3.6. Estado de salud del servicio

MS05 dispone del endpoint:

`GET /actuator/health`

Cuando el servicio se encuentra disponible, responde:

```json
{
  "status": "UP"
}
```

Este endpoint se consulta únicamente dentro de la máquina virtual. No se encuentra publicado a través del API Gateway.

## 4. Pruebas y calidad del código

Para ejecutar las pruebas y verificar la calidad del proyecto se utiliza el siguiente comando desde la raíz del repositorio:

```bash
./mvnw verify
```

Este proceso ejecuta las pruebas automatizadas y verifica que se cumplan los criterios de calidad establecidos:

- **Pruebas unitarias:** comprueban el funcionamiento de las diferentes partes del microservicio.
- **Cobertura con JaCoCo:** exige una cobertura mínima del 80 % de líneas de código.
- **Análisis con SpotBugs:** verifica que no existan hallazgos reportados por esta herramienta.

El pipeline de integración continua ejecuta estas mismas verificaciones para validar los cambios antes de su integración.

## 5. Integración y despliegue continuo (CI/CD)

MS05 utiliza GitHub Actions para automatizar las pruebas, los análisis de calidad, la publicación de imágenes Docker y los despliegues en los ambientes de prueba y producción.

Los workflows principales son:

| Workflow | Cuándo se ejecuta | Qué realiza |
|---|---|---|
| `ci-quality.yml` | En cada push y pull request hacia `develop` o `main`. | Ejecuta las pruebas, verifica la cobertura con JaCoCo y realiza el análisis con SpotBugs. Desde `develop` y `main`, publica la imagen Docker en GHCR. |
| `ci-quality.yml` → `deploy.yml` | Después de un push o merge a `develop`. | Despliega la imagen generada en el ambiente de prueba, accesible a través del puerto `8443` del Gateway. |
| `deploy-prod.yml` | Ejecución manual desde la rama `main`. | Despliega en producción una imagen identificada mediante su tag `sha-xxxxxxx`, que ya fue validada en el ambiente de prueba. No reconstruye la imagen. |

### 5.1. Publicación de imágenes Docker

Las imágenes del microservicio se publican en GitHub Container Registry (GHCR), en el siguiente repositorio:

`ghcr.io/campusseguro-dosw-2026-2/ms05-brigades-and-incidents-api`

Se utilizan etiquetas para identificar las versiones:

- `develop`: imagen correspondiente a la rama de desarrollo.
- `latest`: imagen identificada como la versión más reciente de producción.
- `sha-xxxxxxx`: imagen identificada mediante el hash del commit que la generó.

El uso de etiquetas asociadas al commit permite identificar exactamente qué versión se está desplegando y facilita la recuperación de una versión anterior cuando sea necesario.

### 5.2. Despliegue en prueba y producción

El proceso de despliegue diferencia dos ambientes:

- **Prueba:** cada integración de cambios en `develop` genera y publica la imagen Docker y posteriormente la despliega de forma automática.
- **Producción:** el despliegue se inicia manualmente desde GitHub Actions, utilizando una imagen que ya haya sido validada en el ambiente de prueba. El proceso requiere la aprobación correspondiente.

De esta manera, se evita reconstruir una imagen diferente para producción y se garantiza que la versión desplegada sea la misma que pasó por las verificaciones del ambiente de prueba.

## 6. Despliegue en la nube

MS05 se ejecuta en una máquina virtual propia y se comunica con los demás microservicios a través del API Gateway.

La configuración de infraestructura y seguridad sigue la guía del proyecto:

[Conexión al API Gateway](https://github.com/CampusSeguro-DOSW-2026-2/documentation/wiki/Conexi%C3%B3n-al-API-Gateway).

### 6.1. Seguridad de la conexión

La infraestructura aplica las siguientes medidas:

- El firewall permite conexiones desde la IP del API Gateway por los puertos `443` (producción) y `8443` (prueba).
- El acceso administrativo a la máquina virtual se realiza mediante SSH por el puerto `22`, restringido a la IP autorizada del administrador.
- Un proxy Caddy de borde exige el certificado de cliente del Gateway mediante mTLS (autenticación mutua TLS).
- Los ambientes de prueba y producción comparten la máquina virtual, pero mantienen stacks independientes, cada uno con su instancia de MS05, PostgreSQL, archivos de evidencias y secretos.

### 6.2. Configuración inicial de la máquina virtual

Los siguientes pasos se realizan una sola vez. Los comandos identificados con `(VM)` deben ejecutarse dentro de la máquina virtual mediante SSH.

#### Paso 1. Configuración del proxy Caddy (mTLS)

El equipo del API Gateway proporciona una carpeta `certs/` con los certificados necesarios. Esta carpeta contiene los directorios `prod/` y `test/`, cada uno con los archivos:

- `servidor.crt`
- `servidor.key`
- `ca.pem`

Estos certificados son información sensible y no deben almacenarse en el repositorio.

En la máquina virtual:

```bash
curl -fsSL https://get.docker.com | sudo sh && sudo usermod -aG docker "$USER"
```

Después de ejecutar el comando, salir de la sesión SSH y volver a ingresar para aplicar los permisos del grupo Docker.

Crear el directorio de infraestructura:

```bash
mkdir -p ~/campusseguro/edge && cd ~/campusseguro/edge
```

Descargar los archivos de configuración del Gateway:

```bash
curl -fsSLO https://raw.githubusercontent.com/CampusSeguro-DOSW-2026-2/api-gateway/develop/infra/caddy/Caddyfile.microservicio

curl -fsSLO https://raw.githubusercontent.com/CampusSeguro-DOSW-2026-2/api-gateway/develop/infra/caddy/docker-compose.microservicio.yml
```

Configurar los servicios internos:

```bash
printf 'UPSTREAM_PROD=ms05-prod:8085\nUPSTREAM_TEST=ms05-test:8085\n' > .env
```

Proteger las claves privadas de los certificados:

```bash
chmod 600 certs/*/servidor.key
```

Iniciar el proxy:

```bash
docker compose -f docker-compose.microservicio.yml up -d
```

#### Paso 2. Configuración del Runner self-hosted

Para ejecutar los despliegues dentro de la máquina virtual, se configura un runner propio de GitHub Actions.

En el repositorio de MS05, ingresar a:

**Settings → Actions → Runners → New self-hosted runner**

Seleccionar:
- Sistema operativo: Linux.
- Arquitectura: x64.
- Etiqueta: `ms05`.

Instalar y configurar el runner como servicio dentro de la máquina virtual:

```bash
./config.sh --url https://github.com/CampusSeguro-DOSW-2026-2/ms05-brigades-and-incidents --token <TOKEN> --labels ms05 --unattended
```

Después, instalar e iniciar el servicio:

```bash
sudo ./svc.sh install "$USER" && sudo ./svc.sh start
```

El token debe obtenerse desde GitHub durante la configuración del runner.

#### Paso 3. Configuración de los ambientes de GitHub

Se deben crear dos environments en GitHub Actions:

| Ambiente | Configuración |
|---|---|
| `ms05-test` | Permitir despliegues únicamente desde la rama `develop`. |
| `ms05-prod` | Permitir despliegues únicamente desde `main` y exigir aprobación de los revisores configurados. |

En ambos ambientes se deben configurar las siguientes variables y secretos:

| Nombre | Tipo | Descripción |
|---|---|---|
| `GATEWAY_URL` | Variable | URL del API Gateway. Para prueba utiliza `https://campusseguro-gw.northcentralus.cloudapp.azure.com:8443`. En producción utiliza la misma dirección sin `:8443`. |
| `MS05_EXTRA_ENV` | Variable opcional | Permite definir variables de entorno adicionales para MS05 mediante líneas `CLAVE=valor`. |
| `DEPLOY_DIR` | Variable opcional | Directorio donde se instala el servicio. Por defecto: `~/campusseguro/ms05`. |
| `DB_PASSWORD` | Secreto | Contraseña de PostgreSQL. Se genera con `openssl rand -base64 24` y debe ser diferente para cada ambiente. Solo se aplica al crear la base de datos. |
| `GATEWAY_SERVICE_KEY` | Secreto | Clave de servicio entregada por el equipo del API Gateway para el ambiente correspondiente. Se utiliza como `GW_SERVICE_KEY_MS05`. |

#### Paso 4. Ejecución de los despliegues

Una vez configurados los ambientes y el runner, se pueden realizar los despliegues:

**Ambiente de prueba**

Cada merge a `develop` ejecuta el proceso de publicación y despliegue de manera automática mediante el job `desplegar-test`.

**Ambiente de producción**

Después de integrar los cambios en `main`:

1. Ingresar a **GitHub → Actions**.
2. Seleccionar el workflow **Desplegar a producción**.
3. Presionar **Run workflow**.
4. Seleccionar la rama `main`.
5. Indicar el tag de la imagen que ya fue validada en prueba.
6. Aprobar el despliegue cuando GitHub lo solicite.

### 6.3. Verificación y operación del servicio

El script `infra/deploy.sh` realiza verificaciones durante el despliegue dentro de la máquina virtual:

- Comprueba que el endpoint de salud responda correctamente con HTTP 200 desde la red interna `campus-edge`.
- Verifica que el proxy de borde rechace las conexiones que no presenten el certificado del API Gateway.

La inicialización del esquema de PostgreSQL se realiza mediante:

`infra/db/01-initialize-ms05.sh`

Este script utiliza los archivos SQL ubicados en `database/init` y `database/migrations`. La inicialización se ejecuta únicamente cuando se crea la base de datos por primera vez.

Para revisar el estado de los contenedores y consultar los registros de ejecución, ingresar al directorio del ambiente correspondiente. Por ejemplo, para prueba:

```bash
cd ~/campusseguro/ms05/test
```

Consultar los contenedores:

```bash
docker compose ps
```

Consultar los últimos registros del servicio:

```bash
docker compose logs --tail=100 ms05
```

**Importante:** no utilizar `docker compose down -v` durante las tareas normales de operación, ya que esta instrucción elimina los volúmenes y puede provocar la pérdida de la base de datos y de los archivos de evidencias.

---

## 7. Consideraciones finales

MS05 mantiene la información de su dominio en una base de datos independiente y se comunica con los demás microservicios mediante las APIs expuestas a través del API Gateway.

Su diseño permite gestionar el ciclo operativo de los brigadistas y los incidentes, conservar el historial de las acciones realizadas y mantener la trazabilidad de los eventos. Además, los procesos automatizados de pruebas, análisis de calidad y despliegue facilitan la integración continua y la publicación controlada de nuevas versiones en los ambientes de prueba y producción.