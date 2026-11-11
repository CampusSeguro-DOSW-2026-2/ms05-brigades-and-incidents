
package edu.eci.dosw.brigades_and_incidents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AsignacionPorDefectoRequest(

        @NotBlank(message = "El brigadista es obligatorio")
        String brigadistaId,

        @NotBlank(message = "El evento es obligatorio")
        String eventoId,

        @NotBlank(message = "La ubicación es obligatoria")
        String ubicacionId,

        @NotBlank(message = "El tipo de ubicación es obligatorio")
        String tipoUbicacion,

        @Size(max = 5000)
        String observaciones
) {
}
