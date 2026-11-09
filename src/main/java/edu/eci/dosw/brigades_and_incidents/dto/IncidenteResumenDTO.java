package edu.eci.dosw.brigades_and_incidents.dto;

import java.time.OffsetDateTime;

public record IncidenteResumenDTO(

        String incidenteId,

        String zonaId,

        String estado,

        OffsetDateTime reportadoEn

) {
}