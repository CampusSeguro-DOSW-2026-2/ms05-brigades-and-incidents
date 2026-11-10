package edu.eci.dosw.brigades_and_incidents.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RegistroAuditoriaResponse(
        UUID idRegistro,
        String tipoRegistro,
        String usuarioOrigen,
        String accion,
        String recursoTipo,
        String recursoId,
        OffsetDateTime fechaHoraRegistro,
        String eventoId,
        String origen,
        String resultado,
        String detalle
) {
}