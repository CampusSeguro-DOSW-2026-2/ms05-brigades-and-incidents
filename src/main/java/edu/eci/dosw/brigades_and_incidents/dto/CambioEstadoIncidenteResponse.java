package edu.eci.dosw.brigades_and_incidents.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CambioEstadoIncidenteResponse(

        UUID incidenteId,

        String estadoAnterior,

        String estadoNuevo,

        String responsable,

        OffsetDateTime fechaCambio

) {
}
