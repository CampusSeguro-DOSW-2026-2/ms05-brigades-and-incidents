package edu.eci.dosw.brigades_and_incidents.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Cambios de un incidente abierto. Los dos campos son opcionales: el que no se envía queda igual.
 *
 * @param descripcion nueva descripción
 * @param prioridadId nueva prioridad del catálogo de MS05; la usa la persona que completa la solicitud de asistencia
 *                    que MS04 creó desde el check-in con la prioridad por defecto
 */
public record EditarIncidenteRequest(

        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.")
        String descripcion,

        @Positive(message = "La prioridad seleccionada no existe.")
        Long prioridadId

) {
}
