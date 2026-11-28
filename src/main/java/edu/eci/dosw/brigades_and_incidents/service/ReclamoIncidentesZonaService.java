package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.entity.EstadoIncidente;
import edu.eci.dosw.brigades_and_incidents.entity.HistorialIncidente;
import edu.eci.dosw.brigades_and_incidents.entity.Incidente;
import edu.eci.dosw.brigades_and_incidents.event.IncidenteAuditPublisher;
import edu.eci.dosw.brigades_and_incidents.repository.EstadoIncidenteRepository;
import edu.eci.dosw.brigades_and_incidents.repository.HistorialIncidenteRepository;
import edu.eci.dosw.brigades_and_incidents.repository.IncidenteRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Reclama para un brigadista los incidentes que quedaron reportados sin responsable en la zona a la que acaba de
 * asignarse. Así un incidente creado antes de la autoasignación también pasa a su cargo y el panel muestra
 * «Atender», igual que si se hubiera creado después.
 */
@Service
public class ReclamoIncidentesZonaService {

    private static final String REPORTADO = "REPORTADO";
    private static final String ASIGNADO = "ASIGNADO";

    private final IncidenteRepository incidenteRepository;
    private final EstadoIncidenteRepository estadoRepository;
    private final HistorialIncidenteRepository historialRepository;
    private final IncidenteAuditPublisher auditPublisher;

    public ReclamoIncidentesZonaService(
            IncidenteRepository incidenteRepository,
            EstadoIncidenteRepository estadoRepository,
            HistorialIncidenteRepository historialRepository,
            IncidenteAuditPublisher auditPublisher
    ) {
        this.incidenteRepository = incidenteRepository;
        this.estadoRepository = estadoRepository;
        this.historialRepository = historialRepository;
        this.auditPublisher = auditPublisher;
    }

    /**
     * Asigna el brigadista a los incidentes REPORTADO de la zona en el evento y registra su historial y auditoría.
     *
     * @param eventoId evento del incidente
     * @param brigadistaId identificador externo del brigadista que se asigna a la zona
     * @param ubicacionId zona del evento a la que se asignó
     * @return cantidad de incidentes reclamados
     */
    @Transactional
    public int reclamarReportados(
            String eventoId,
            String brigadistaId,
            String ubicacionId
    ) {
        if (eventoId == null || eventoId.isBlank()
                || brigadistaId == null || brigadistaId.isBlank()
                || ubicacionId == null || ubicacionId.isBlank()) {
            return 0;
        }

        List<Incidente> pendientes = incidenteRepository
                .findByEventoIdAndUbicacionId(eventoId, ubicacionId)
                .stream()
                .filter(incidente -> !incidente.isEliminado())
                .filter(this::estaReportadoSinResponsable)
                .toList();

        if (pendientes.isEmpty()) {
            return 0;
        }

        EstadoIncidente asignado = estadoRepository.findByNombre(ASIGNADO)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encuentra el estado " + ASIGNADO + " del incidente."
                ));

        for (Incidente incidente : pendientes) {
            EstadoIncidente anterior = incidente.getEstado();

            incidente.setEstado(asignado);
            incidente.setBrigadistaResponsableId(brigadistaId);
            Incidente actualizado = incidenteRepository.save(incidente);

            HistorialIncidente historial = new HistorialIncidente();
            historial.setIncidente(actualizado);
            historial.setEstadoAnterior(anterior);
            historial.setEstadoNuevo(asignado);
            historial.setResponsableAnteriorId(null);
            historial.setResponsableNuevoId(brigadistaId);
            historial.setActorId(brigadistaId);
            historial.setObservaciones(
                    "Asignación automática al brigadista que eligió la zona."
            );
            historialRepository.save(historial);

            auditPublisher.publicar(
                    "ASIGNAR_RESPONSABLE",
                    "INCIDENTE",
                    incidente.getId().toString(),
                    eventoId,
                    brigadistaId,
                    "EXITOSO",
                    "Se asignó el brigadista de la zona al incidente reportado.",
                    Map.of("ubicacion_id", ubicacionId)
            );
        }

        return pendientes.size();
    }

    private boolean estaReportadoSinResponsable(Incidente incidente) {
        if (incidente.getEstado() == null
                || incidente.getEstado().getNombre() == null
                || !REPORTADO.equalsIgnoreCase(incidente.getEstado().getNombre())) {
            return false;
        }

        String responsable = incidente.getBrigadistaResponsableId();
        return responsable == null || responsable.isBlank();
    }
}
