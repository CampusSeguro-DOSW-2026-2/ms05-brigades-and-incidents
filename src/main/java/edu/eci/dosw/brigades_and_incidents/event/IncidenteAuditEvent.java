
package edu.eci.dosw.brigades_and_incidents.event;

import java.time.OffsetDateTime;

public record IncidenteAuditEvent(
        String accion,
        String recurso,
        String recursoId,
        String eventoId,
        String usuarioId,
        String resultado,
        String detalle,
        OffsetDateTime fecha
) {
}
