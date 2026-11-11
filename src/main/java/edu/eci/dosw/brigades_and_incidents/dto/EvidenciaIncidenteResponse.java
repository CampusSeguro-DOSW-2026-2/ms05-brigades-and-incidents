package edu.eci.dosw.brigades_and_incidents.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EvidenciaIncidenteResponse(

        UUID id,

        String nombreArchivo,

        String tipoContenido,

        String referenciaArchivo,

        OffsetDateTime fechaCarga

) {
}