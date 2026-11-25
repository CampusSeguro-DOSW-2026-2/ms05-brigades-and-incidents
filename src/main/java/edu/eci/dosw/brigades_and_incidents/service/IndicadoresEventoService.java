package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.dto.BrigadistaIndicadorDTO;
import edu.eci.dosw.brigades_and_incidents.dto.IncidenteResumenDTO;
import edu.eci.dosw.brigades_and_incidents.dto.IndicadoresEventoResponse;
import edu.eci.dosw.brigades_and_incidents.entity.Incidente;
import edu.eci.dosw.brigades_and_incidents.repository.IncidenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Indicadores de incidentes de un evento para MS06 (monitoreo en vivo y evaluación al finalizar). Se calculan con los
 * incidentes del evento que no fueron eliminados:
 * <ul>
 *   <li>atendido: ya tuvo primera atención (EN_ATENCION, RESUELTO o CERRADO);</li>
 *   <li>resuelto: RESUELTO o CERRADO;</li>
 *   <li>tiempo promedio de atención: del reporte a la primera atención, de los casos atendidos.</li>
 * </ul>
 * Por brigadista cuentan los casos de los que es responsable.
 */
@Service
public class IndicadoresEventoService {

    private static final Set<String> ATENDIDOS = Set.of("EN_ATENCION", "RESUELTO", "CERRADO");
    private static final Set<String> RESUELTOS = Set.of("RESUELTO", "CERRADO");

    private final IncidenteRepository incidenteRepository;

    public IndicadoresEventoService(IncidenteRepository incidenteRepository) {
        this.incidenteRepository = incidenteRepository;
    }

    @Transactional(readOnly = true)
    public IndicadoresEventoResponse consultar(String eventoId) {
        List<Incidente> incidentes = incidenteRepository.findByEventoId(eventoId).stream()
                .filter(incidente -> !incidente.isEliminado())
                .sorted(Comparator.comparing(Incidente::getFechaReporte,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        long atendidos = incidentes.stream().filter(IndicadoresEventoService::atendido).count();
        long resueltos = incidentes.stream().filter(IndicadoresEventoService::resuelto).count();

        Map<String, long[]> porBrigadista = new LinkedHashMap<>();
        for (Incidente incidente : incidentes) {
            String responsable = incidente.getBrigadistaResponsableId();
            if (responsable == null || responsable.isBlank()) {
                continue;
            }
            long[] casos = porBrigadista.computeIfAbsent(responsable, id -> new long[2]);
            if (atendido(incidente)) {
                casos[0]++;
            }
            if (resuelto(incidente)) {
                casos[1]++;
            }
        }

        return new IndicadoresEventoResponse(
                eventoId,
                OffsetDateTime.now(),
                incidentes.size(),
                atendidos,
                resueltos,
                porBrigadista.entrySet().stream()
                        .map(e -> new BrigadistaIndicadorDTO(e.getKey(), e.getValue()[0], e.getValue()[1]))
                        .toList(),
                incidentes.stream().map(IndicadoresEventoService::resumen).toList(),
                promedioAtencion(incidentes)
        );
    }

    private static String estado(Incidente incidente) {
        return incidente.getEstado() == null ? "" : incidente.getEstado().getNombre();
    }

    private static boolean atendido(Incidente incidente) {
        return incidente.getFechaPrimeraAtencion() != null || ATENDIDOS.contains(estado(incidente));
    }

    private static boolean resuelto(Incidente incidente) {
        return RESUELTOS.contains(estado(incidente));
    }

    private static Long promedioAtencion(List<Incidente> incidentes) {
        List<Long> segundos = incidentes.stream()
                .filter(i -> i.getFechaReporte() != null && i.getFechaPrimeraAtencion() != null)
                .map(i -> Math.max(0, Duration.between(i.getFechaReporte(), i.getFechaPrimeraAtencion()).toSeconds()))
                .toList();
        if (segundos.isEmpty()) {
            return null;
        }
        return Math.round(segundos.stream().mapToLong(Long::longValue).average().orElse(0));
    }

    private static IncidenteResumenDTO resumen(Incidente incidente) {
        return new IncidenteResumenDTO(
                incidente.getId() == null ? null : incidente.getId().toString(),
                incidente.getEventoId(),
                incidente.getUbicacionId(),
                incidente.getTipoUbicacion() == null ? null : incidente.getTipoUbicacion().getNombre(),
                incidente.getPrioridad() == null ? null : incidente.getPrioridad().getNombre(),
                estado(incidente),
                incidente.getBrigadistaResponsableId(),
                incidente.getFechaReporte()
        );
    }
}
