
package edu.eci.dosw.brigades_and_incidents.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AsignacionResponse(
        UUID idAsignacion,
        String brigadistaId,
        String eventoId,
        String ubicacionId,
        String tipoUbicacion,
        String estado,
        String observaciones,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}
