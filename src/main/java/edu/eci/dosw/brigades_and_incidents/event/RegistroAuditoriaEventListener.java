package edu.eci.dosw.brigades_and_incidents.event;

import edu.eci.dosw.brigades_and_incidents.client.AuditoriaClient;
import edu.eci.dosw.brigades_and_incidents.client.AuditoriaClient.EventoAuditoria;
import edu.eci.dosw.brigades_and_incidents.service.RegistroAuditoriaService;
import edu.eci.dosw.brigades_and_incidents.service.TipoEventoResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class RegistroAuditoriaEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(RegistroAuditoriaEventListener.class);

    private final RegistroAuditoriaService auditoriaService;
    private final AuditoriaClient auditoriaClient;
    private final TipoEventoResolver tipoEventoResolver;

    public RegistroAuditoriaEventListener(
            RegistroAuditoriaService auditoriaService
    ) {
        this(auditoriaService, null, null);
    }

    @Autowired
    public RegistroAuditoriaEventListener(
            RegistroAuditoriaService auditoriaService,
            AuditoriaClient auditoriaClient,
            TipoEventoResolver tipoEventoResolver
    ) {
        this.auditoriaService = auditoriaService;
        this.auditoriaClient = auditoriaClient;
        this.tipoEventoResolver = tipoEventoResolver;
    }

    @EventListener
    public void escucharAsignacion(AsignacionAuditEvent evento) {
        OffsetDateTime fecha = evento.fecha() == null
                ? null
                : evento.fecha().atOffset(
                        OffsetDateTime.now().getOffset()
                );

        String detalle = "Accion realizada sobre la asignacion del brigadista: "
                + evento.brigadistaId();
        String recursoId = evento.idAsignacion().toString();

        auditoriaService.registrarAccion(
                "ASIGNACION",
                evento.actorId(),
                evento.accion(),
                "ASIGNACION",
                recursoId,
                evento.eventoId(),
                "MS05",
                "EXITOSO",
                detalle,
                fecha
        );

        Map<String, Object> atributos = new LinkedHashMap<>();
        if (evento.brigadistaId() != null && !evento.brigadistaId().isBlank()) {
            atributos.put("brigadista_id", evento.brigadistaId());
        }

        enviarAMs08(
                evento.actorId(),
                evento.accion(),
                "ASIGNACION",
                recursoId,
                "EXITOSO",
                fecha,
                detalle,
                evento.eventoId(),
                atributos
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

        enviarAMs08(
                evento.usuarioId(),
                evento.accion(),
                evento.recurso(),
                evento.recursoId(),
                evento.resultado(),
                evento.fecha(),
                evento.detalle(),
                evento.eventoId(),
                evento.atributos()
        );
    }

    /**
     * Reenvía el hecho a MS08. Un fallo de comunicación nunca interrumpe ni revierte la operación en MS05.
     */
    private void enviarAMs08(
            String usuarioId,
            String accion,
            String recursoTipo,
            String recursoId,
            String resultado,
            OffsetDateTime fechaHora,
            String detalle,
            String eventoId,
            Map<String, Object> atributos
    ) {
        if (auditoriaClient == null) {
            return;
        }
        try {
            Boolean esSimulacro = resolverSimulacro(eventoId).orElse(null);
            auditoriaClient.registrarEvento(new EventoAuditoria(
                    usuarioId,
                    accion,
                    recursoTipo,
                    recursoId,
                    resultado,
                    fechaHora,
                    detalle,
                    eventoId,
                    esSimulacro,
                    atributos
            ));
        } catch (RuntimeException exception) {
            LOG.warn("No se pudo enviar el hecho de auditoría {} ({}) a MS08: {}",
                    accion, recursoId, exception.getMessage());
        }
    }

    private Optional<Boolean> resolverSimulacro(String eventoId) {
        if (tipoEventoResolver == null || eventoId == null || eventoId.isBlank()) {
            return Optional.empty();
        }
        try {
            return tipoEventoResolver.esSimulacro(eventoId);
        } catch (RuntimeException exception) {
            LOG.warn("No se pudo determinar si el evento {} es simulacro para auditoría MS08: {}",
                    eventoId, exception.getMessage());
            return Optional.empty();
        }
    }
}