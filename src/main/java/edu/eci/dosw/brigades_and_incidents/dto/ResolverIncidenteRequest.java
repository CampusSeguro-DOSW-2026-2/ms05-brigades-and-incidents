package edu.eci.dosw.brigades_and_incidents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolverIncidenteRequest(

        @NotBlank(message = "Las observaciones de resolución son obligatorias.")
        @Size(max = 1000, message = "Las observaciones no pueden superar 1000 caracteres.")
        String observaciones,

        // Resultado de la atención: RESUELTO, TRANSFERIDO o NO_RESUELTO. Si no llega, se registra RESUELTO.
        String resultado

) {
}
