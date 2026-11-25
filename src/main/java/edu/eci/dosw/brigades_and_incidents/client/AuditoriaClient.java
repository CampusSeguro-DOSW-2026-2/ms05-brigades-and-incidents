package edu.eci.dosw.brigades_and_incidents.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cliente para enviar hechos de auditoría y analítica de MS05 hacia MS08 (POST /api/v1/audit/events).
 */
public interface AuditoriaClient {

    void registrarEvento(EventoAuditoria evento);

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "El mapa de atributos se almacena como copia inmutable en el constructor."
    )
    record EventoAuditoria(
            String usuarioId,
            String accion,
            String recursoTipo,
            String recursoId,
            String resultado,
            OffsetDateTime fechaHora,
            String detalle,
            String eventoId,
            Boolean esSimulacro,
            Map<String, Object> atributos
    ) {
        public EventoAuditoria {
            atributos = atributos == null || atributos.isEmpty()
                    ? Map.of()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(atributos));
        }
    }
}