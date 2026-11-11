package edu.eci.dosw.brigades_and_incidents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearIncidenteRequest(

        @Size(max = 150, message = "El nombre del afectado no puede superar 150 caracteres")
        String nombreAfectado,

        @NotNull(message = "La prioridad es obligatoria")
        Long prioridadId,

        @NotBlank(message = "La ubicación es obligatoria")
        String ubicacionId,

        @NotBlank(message = "El tipo de ubicación es obligatorio")
        String tipoUbicacion,

        @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
        String descripcion

) {
}