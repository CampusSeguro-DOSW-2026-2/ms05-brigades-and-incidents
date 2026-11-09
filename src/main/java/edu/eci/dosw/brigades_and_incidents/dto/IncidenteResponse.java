package edu.eci.dosw.brigades_and_incidents.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IncidenteResponse(

        UUID idIncidente,

        String eventoId,

        String nombreAfectado,

        String reportanteId,

        String ubicacionId,

        String tipoUbicacion,

        String prioridad,

        String estado,

        String descripcion,

        OffsetDateTime fechaReporte

) {
}