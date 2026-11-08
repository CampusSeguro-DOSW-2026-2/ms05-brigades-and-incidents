package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.dto.BrigadistaAsignadoDTO;
import edu.eci.dosw.brigades_and_incidents.dto.BrigadistasEventoResponse;
import edu.eci.dosw.brigades_and_incidents.entity.AsignacionBrigadista;
import edu.eci.dosw.brigades_and_incidents.repository.AsignacionBrigadistaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ConsultaBrigadistasEventoService {

    private final AsignacionBrigadistaRepository asignacionRepository;

    public ConsultaBrigadistasEventoService(
            AsignacionBrigadistaRepository asignacionRepository
    ) {
        this.asignacionRepository = asignacionRepository;
    }

    @Transactional(readOnly = true)
    public BrigadistasEventoResponse consultar(String eventoId) {

        List<AsignacionBrigadista> asignaciones =
                asignacionRepository.findByEventoIdAndEstado_Nombre(
                        eventoId,
                        "VIGENTE"
                );

        List<BrigadistaAsignadoDTO> brigadistas = asignaciones.stream()
                .map(asignacion -> new BrigadistaAsignadoDTO(
                        asignacion.getBrigadistaId(),
                        null,
                        asignacion.getUbicacionId(),
                        "ACTIVO"
                ))
                .toList();

        return new BrigadistasEventoResponse(
                eventoId,
                OffsetDateTime.now(),
                brigadistas
        );
    }
}