package edu.eci.dosw.brigades_and_incidents.dto;

import jakarta.validation.constraints.Size;

public record EditarIncidenteRequest(

        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.")
        String descripcion

) {
}
