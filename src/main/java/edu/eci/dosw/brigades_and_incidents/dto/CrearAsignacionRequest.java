
package edu.eci.dosw.brigades_and_incidents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearAsignacionRequest(

        @NotBlank(message = "El identificador del brigadista es obligatorio")
        String brigadistaId,

        @NotBlank(message = "El identificador del evento es obligatorio")
        String eventoId,

        @NotBlank(message = "El identificador de la ubicación es obligatorio")
        String ubicacionId,

        @NotBlank(message = "El tipo de ubicación es obligatorio")
        String tipoUbicacion,

        @Size(max = 5000, message = "Las observaciones son demasiado largas")
        String observaciones
) {
}
