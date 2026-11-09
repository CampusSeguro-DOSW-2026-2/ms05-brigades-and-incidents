
package edu.eci.dosw.brigades_and_incidents.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record AsignacionAuditEvent(
        UUID idAsignacion,
        String accion,
        String actorId,
        String brigadistaId,
        String eventoId,
        LocalDateTime fecha
) {
}
