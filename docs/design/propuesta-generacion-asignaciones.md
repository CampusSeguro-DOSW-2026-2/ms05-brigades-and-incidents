# Propuesta: generación y confirmación de asignaciones de brigadistas

**Estado:** flujo definido e implementado en MS05, pendiente de integración completa con MS06.

**Microservicios involucrados:** MS05 Brigadistas e Incidentes y MS06 Eventos.

## 1. Contexto

Cuando se crea un evento en MS06, es necesario distribuir los brigadistas disponibles entre las ubicaciones seleccionadas para atender las necesidades del evento.

MS05 es responsable de administrar los brigadistas, consultar sus disponibilidades, generar una propuesta de distribución y validar y guardar las asignaciones definitivas.

MS06 permite al organizador revisar la propuesta y realizar modificaciones antes de confirmar la distribución.

## 2. Configuración de asignaciones predeterminadas

MS05 utiliza una configuración global que establece la cantidad base de brigadistas requerida según el tipo de ubicación.

| Tipo de ubicación | Cantidad predeterminada |
|---|---:|
| EDIFICIO | 4 |
| ESPACIO | 3 |
| ZONA_SEGURA | 3 |

Estas cantidades sirven como referencia para generar la propuesta automática.

La configuración se almacena en MS05 y puede consultarse al momento de generar una distribución.

## 3. Flujo de generación y confirmación

El proceso se divide en tres etapas:

### A. Solicitud de propuesta desde MS06

Cuando se inicia el proceso de asignación de un evento, MS06 envía a MS05:

- Identificador del evento.
- Identificadores de las ubicaciones seleccionadas.
- Tipo de cada ubicación.

MS05 consulta las configuraciones correspondientes y obtiene los brigadistas disponibles.

Para considerar disponible a un brigadista, debe cumplir estas condiciones:

- Su estado operativo debe ser ACTIVO.
- No debe tener otra asignación en estado VIGENTE.

Con esta información, MS05 genera una propuesta de distribución.

### B. Revisión y modificación en MS06

MS05 devuelve la propuesta a MS06 sin guardar todavía las asignaciones definitivas en la base de datos.

La respuesta incluye:

- Brigadistas propuestos para cada ubicación.
- Brigadistas que quedaron sin asignar.
- Total de brigadistas disponibles.
- Total de brigadistas requeridos.
- Indicador de si fue necesario ajustar la distribución.

MS06 presenta esta información al organizador, quien puede aceptar la propuesta o realizar modificaciones, como cambiar un brigadista de ubicación o ajustar la distribución.

### C. Confirmación de asignaciones

Una vez revisada la propuesta, MS06 envía a MS05 la distribución definitiva.

MS05 valida:

- Que los brigadistas existan.
- Que los brigadistas estén activos.
- Que ningún brigadista tenga otra asignación vigente.
- Que los identificadores y tipos de ubicación sean válidos.
- Que no existan brigadistas duplicados en la solicitud.

Si todas las validaciones se cumplen, MS05 guarda las asignaciones definitivas.

El tipo de asignación se determina automáticamente:

- `DEFAULT`: cuando el brigadista conserva la ubicación propuesta originalmente.
- `MANUAL`: cuando el organizador modifica la asignación propuesta.

## 4. Manejo de propuestas temporales

Las propuestas generadas se almacenan temporalmente en memoria dentro de MS05, asociadas al identificador del evento.

Esto permite comparar la distribución original con la distribución definitiva enviada por MS06.

Consideraciones:

- La propuesta no representa una asignación definitiva.
- La propuesta se elimina después de confirmar correctamente las asignaciones.
- Si MS05 se reinicia antes de la confirmación, las propuestas almacenadas en memoria se pierden.
- Si MS06 intenta confirmar un evento sin una propuesta pendiente, MS05 responde con un conflicto HTTP 409 y se debe generar una nueva propuesta.

## 5. Responsabilidades de cada microservicio

| MS05 - Brigadistas e Incidentes | MS06 - Eventos |
|---|---|
| Administrar los brigadistas. | Administrar los eventos. |
| Mantener las configuraciones predeterminadas. | Enviar las ubicaciones seleccionadas del evento. |
| Consultar la disponibilidad de brigadistas. | Solicitar la generación de la propuesta. |
| Generar la distribución automática. | Mostrar la propuesta al organizador. |
| Validar la distribución definitiva. | Permitir modificaciones manuales a la propuesta. |
| Clasificar las asignaciones como DEFAULT o MANUAL. | Enviar la distribución definitiva a MS05. |
| Persistir las asignaciones confirmadas. | Consultar las asignaciones del evento cuando sea necesario. |

## 6. Consideraciones de negocio

- Un brigadista solamente puede tener una asignación vigente a la vez, independientemente del evento.
- Los brigadistas inactivos no participan en la generación de propuestas.
- La propuesta automática utiliza las cantidades configuradas por tipo de ubicación.
- El organizador puede modificar la propuesta antes de confirmarla.
- MS05 es responsable de validar y persistir la distribución definitiva.
- Las modificaciones posteriores de una asignación vigente deben quedar registradas en el historial de asignaciones.

## 7. Decisiones pendientes

- Definir con el equipo el mecanismo de persistencia de propuestas temporales para evitar su pérdida ante reinicios de MS05.
- Acordar el contrato definitivo de integración entre MS05 y MS06.
- Definir si las cantidades predeterminadas globales podrán tener ajustes particulares por evento en futuras versiones.
