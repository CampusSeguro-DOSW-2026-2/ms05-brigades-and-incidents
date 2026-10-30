# Catálogo de errores - MS05 Brigadistas e Incidentes

## Formato estándar de respuestas de error

MS05 utiliza el siguiente formato para las respuestas de error:

```json
{
  "timestamp": "2026-10-29T10:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "El brigadista ya tiene una asignación vigente"
}
```

Los campos representan:

- `timestamp`: fecha y hora en que ocurrió el error.
- `status`: código HTTP de la respuesta.
- `error`: descripción del estado HTTP.
- `message`: explicación del error.

## Catálogo

| HTTP | Excepción | Cuándo ocurre | Mensaje esperado |
|---|---|---|---|
| 400 | `IllegalArgumentException` | Los datos enviados son nulos, incompletos o inválidos. También cuando se utiliza un tipo de ubicación o un estado no válido. | El mensaje específico de validación. |
| 404 | `RecursoNoEncontradoException` | No existe el brigadista, la configuración u otro recurso solicitado. | El recurso solicitado no existe. |
| 409 | `ConflictoOperacionException` | Existe un conflicto con el estado actual de la operación, por ejemplo, no hay una propuesta temporal pendiente para confirmar. | No es posible realizar la operación en el estado actual. |
| Según corresponda | `ResponseStatusException` | Se produce una excepción HTTP explícita desde un controlador o servicio. | El mensaje asociado al estado HTTP. |
| 500 | `Exception` | Ocurre un error inesperado que no fue contemplado por las validaciones anteriores. | Ocurrió un error interno en el servidor. |

## Errores relacionados con brigadistas

| Situación | HTTP esperado | Descripción |
|---|---|---|
| Brigadista inexistente | 404 | El identificador enviado no corresponde a un brigadista registrado. |
| Brigadista inactivo | 400 | Se intenta asignar un brigadista cuyo estado operativo no es ACTIVO. |
| Brigadista con asignación vigente | 400 | Se intenta asignar un brigadista que ya tiene una asignación vigente. |

## Errores relacionados con asignaciones

| Situación | HTTP esperado | Descripción |
|---|---|---|
| Datos de asignación inválidos | 400 | Faltan identificadores o datos obligatorios. |
| Ubicación duplicada en la solicitud | 400 | Una misma ubicación aparece más de una vez al generar la propuesta. |
| Propuesta temporal inexistente | 409 | Se intenta confirmar una distribución sin una propuesta pendiente para el evento. |
| Asignación inexistente | 404 | No se encuentra la asignación que se desea consultar o modificar. |
| Modificación de asignación no vigente | 400 | Se intenta modificar una asignación que no está en estado VIGENTE. |

## Errores relacionados con configuraciones

| Situación | HTTP esperado | Descripción |
|---|---|---|
| Configuración inexistente | 404 | No existe una configuración para el tipo de ubicación solicitado. |
| Configuración duplicada | 409 | Se intenta registrar una configuración para un tipo de ubicación que ya tiene una. |
| Cantidad inválida | 400 | La cantidad de brigadistas configurada no cumple las validaciones del servicio. |

## Consideraciones

- Los códigos HTTP deben mantenerse consistentes para que MS06 pueda interpretar correctamente las respuestas.
- El campo `message` permite identificar la causa específica del error.
- Los errores inesperados deben registrarse para facilitar su diagnóstico.
- Este catálogo describe el manejo de excepciones implementado en MS05; no implica que todos los escenarios tengan actualmente una excepción especializada.
