package edu.eci.dosw.brigades_and_incidents.dto;

import jakarta.validation.constraints.NotBlank;

public record AsignarResponsableRequest(

        @NotBlank(message = "El identificador del brigadista es obligatorio.")
        String responsableId,

        String observaciones

) {
}
