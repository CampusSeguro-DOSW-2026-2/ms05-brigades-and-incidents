
package edu.eci.dosw.brigades_and_incidents.client;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuditoriaClient {

    void registrarAccion(RegistroAuditoria solicitud);

    record RegistroAuditoria(
            UUID idAsignacion,
            String actorId,
            String accion,
            String recurso,
            LocalDateTime fecha
    ) {
    }
}
