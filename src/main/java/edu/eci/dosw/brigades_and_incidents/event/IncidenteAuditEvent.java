package edu.eci.dosw.brigades_and_incidents.event;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "El mapa de atributos se almacena como copia inmutable en el constructor."
)
public record IncidenteAuditEvent(
        String accion,
        String recurso,
        String recursoId,
        String eventoId,
        String usuarioId,
        String resultado,
        String detalle,
        OffsetDateTime fecha,
        Map<String, Object> atributos
) {
    public IncidenteAuditEvent {
        atributos = atributos == null || atributos.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(atributos));
    }

    public IncidenteAuditEvent(
            String accion,
            String recurso,
            String recursoId,
            String eventoId,
            String usuarioId,
            String resultado,
            String detalle,
            OffsetDateTime fecha
    ) {
        this(accion, recurso, recursoId, eventoId, usuarioId, resultado, detalle, fecha, Map.of());
    }
}