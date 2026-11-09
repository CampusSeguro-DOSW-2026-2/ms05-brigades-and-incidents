package edu.eci.dosw.brigades_and_incidents.dto;

import java.time.OffsetDateTime;

public record IncidenteResumenDTO(
        String incidenteId,
        String eventoId,
        String zonaId,
        String tipoUbicacion,
        String prioridad,
        String estado,
        OffsetDateTime reportadoEn
) {
}