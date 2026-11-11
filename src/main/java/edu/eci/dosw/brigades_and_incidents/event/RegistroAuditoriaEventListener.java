
package edu.eci.dosw.brigades_and_incidents.event;

import edu.eci.dosw.brigades_and_incidents.service.RegistroAuditoriaService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class RegistroAuditoriaEventListener {

    private final RegistroAuditoriaService auditoriaService;

    public RegistroAuditoriaEventListener(
            RegistroAuditoriaService auditoriaService
    ) {
        this.auditoriaService = auditoriaService;
    }

    @EventListener
    public void escucharAsignacion(AsignacionAuditEvent evento) {
        OffsetDateTime fecha = evento.fecha() == null
                ? null
                : evento.fecha().atOffset(
                        OffsetDateTime.now().getOffset()
                );

        auditoriaService.registrarAccion(
                "ASIGNACION",
                evento.actorId(),
                evento.accion(),
                "ASIGNACION",
                evento.idAsignacion().toString(),
                evento.eventoId(),
                "MS05",
                "EXITOSO",
                "Accion realizada sobre la asignacion del brigadista: "
                        + evento.brigadistaId(),
                fecha
        );
    }

    @EventListener
    public void escucharIncidente(IncidenteAuditEvent evento) {
        String tipoRegistro =
                "EVIDENCIA_INCIDENTE".equalsIgnoreCase(evento.recurso())
                        ? "EVIDENCIA"
                        : "INCIDENTE";

        auditoriaService.registrarAccion(
                tipoRegistro,
                evento.usuarioId(),
                evento.accion(),
                evento.recurso(),
                evento.recursoId(),
                evento.eventoId(),
                "MS05",
                evento.resultado(),
                evento.detalle(),
                evento.fecha()
        );
    }
}
