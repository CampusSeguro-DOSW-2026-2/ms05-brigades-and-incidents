
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
        List<IncidenteResumenDTO> incidentes,
        /** Promedio, en segundos, entre el reporte y la primera atención; null si ningún caso se ha atendido. */
        Long tiempoPromedioAtencionSegundos
) {

    /** Indicadores sin tiempo promedio (ningún caso atendido). */
    public IndicadoresEventoResponse(String eventoId, OffsetDateTime actualizadoEn, long totalIncidentes,
            long totalCasosAtendidos, long totalCasosResueltos, List<BrigadistaIndicadorDTO> porBrigadista,
            List<IncidenteResumenDTO> incidentes) {
        this(eventoId, actualizadoEn, totalIncidentes, totalCasosAtendidos, totalCasosResueltos, porBrigadista,
                incidentes, null);
    }

    public IndicadoresEventoResponse {
        porBrigadista = porBrigadista == null
                ? List.of()
                : List.copyOf(porBrigadista);

        incidentes = incidentes == null
                ? List.of()
                : List.copyOf(incidentes);
    }
}
