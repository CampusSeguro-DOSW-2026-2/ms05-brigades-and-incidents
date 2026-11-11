
package edu.eci.dosw.brigades_and_incidents.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record IndicadoresEventoResponse(
        String eventoId,
        OffsetDateTime actualizadoEn,
        long totalIncidentes,
        long totalCasosAtendidos,
        long totalCasosResueltos,
        List<BrigadistaIndicadorDTO> porBrigadista,
        List<IncidenteResumenDTO> incidentes
) {
    public IndicadoresEventoResponse {
        porBrigadista = porBrigadista == null
                ? List.of()
                : List.copyOf(porBrigadista);

        incidentes = incidentes == null
                ? List.of()
                : List.copyOf(incidentes);
    }
}
