
package edu.eci.dosw.brigades_and_incidents.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record BrigadistasEventoResponse(
        String eventoId,
        OffsetDateTime actualizadoEn,
        List<BrigadistaAsignadoDTO> brigadistas
) {
    public BrigadistasEventoResponse {
        brigadistas = brigadistas == null
                ? List.of()
                : List.copyOf(brigadistas);
    }
}
