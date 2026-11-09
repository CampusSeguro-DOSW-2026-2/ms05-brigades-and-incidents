package edu.eci.dosw.brigades_and_incidents.dto;

import java.util.UUID;

public record AsignacionBrigadistaResumenResponse(
        UUID idAsignacion,
        String eventoId,
        String ubicacionId,
        String tipoUbicacion,
        String estado
) {
}