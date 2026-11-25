package edu.eci.dosw.brigades_and_incidents.dto;

import jakarta.validation.constraints.NotBlank;

/** Edificio del evento elegido por el brigadista (auto asignación) o por el coordinador (cambio de asignación). */
public record SeleccionEdificioRequest(
        @NotBlank(message = "El edificio es obligatorio.")
        String ubicacionId
) {
}
