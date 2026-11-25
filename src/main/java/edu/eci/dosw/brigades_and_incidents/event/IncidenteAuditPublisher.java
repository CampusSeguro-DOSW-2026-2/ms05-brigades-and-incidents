package edu.eci.dosw.brigades_and_incidents.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;

@Component
public class IncidenteAuditPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public IncidenteAuditPublisher(
            ApplicationEventPublisher eventPublisher
    ) {
        this.eventPublisher = eventPublisher;
    }

    public void publicar(
            String accion,
            String recurso,
            String recursoId,
            String eventoId,
            String usuarioId,
            String resultado,
            String detalle
    ) {
        publicar(accion, recurso, recursoId, eventoId, usuarioId, resultado, detalle, Map.of());
    }

    public void publicar(
            String accion,
            String recurso,
            String recursoId,
            String eventoId,
            String usuarioId,
            String resultado,
            String detalle,
            Map<String, Object> atributos
    ) {
        IncidenteAuditEvent evento = new IncidenteAuditEvent(
                accion,
                recurso,
                recursoId,
                eventoId,
                usuarioId,
                resultado,
                detalle,
                OffsetDateTime.now(),
                atributos
        );

        eventPublisher.publishEvent(evento);
    }
}