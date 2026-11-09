package edu.eci.dosw.brigades_and_incidents.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HistorialIncidenteResponse(

        UUID id,

        String estadoAnterior,

        String estadoNuevo,

        String responsableAnterior,

        String responsableNuevo,

        String actorId,

        String observaciones,

        OffsetDateTime fechaCambio

) {
}
