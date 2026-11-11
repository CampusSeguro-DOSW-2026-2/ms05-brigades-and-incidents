package edu.eci.dosw.brigades_and_incidents.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ActivacionBrigadistaResponse(
        UUID idActivacion,
        String brigadistaId,
        String estado,
        OffsetDateTime fechaCambioEstado
) {
}