package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.dto.CambiarEstadoBrigadistaRequest;
import edu.eci.dosw.brigades_and_incidents.dto.ActivacionBrigadistaResponse;
import edu.eci.dosw.brigades_and_incidents.entity.Brigadista;
import edu.eci.dosw.brigades_and_incidents.entity.EstadoBrigadista;
import edu.eci.dosw.brigades_and_incidents.repository.BrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.EstadoBrigadistaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ActivarBrigadistaService {

    private final BrigadistaRepository brigadistaRepository;
    private final EstadoBrigadistaRepository estadoBrigadistaRepository;

    public ActivarBrigadistaService(
            BrigadistaRepository brigadistaRepository,
            EstadoBrigadistaRepository estadoBrigadistaRepository
    ) {
        this.brigadistaRepository = brigadistaRepository;
        this.estadoBrigadistaRepository = estadoBrigadistaRepository;
    }

    @Transactional
    public ActivacionBrigadistaResponse cambiarEstado(
            String brigadistaId,
            CambiarEstadoBrigadistaRequest request
    ) {

        if (brigadistaId == null || brigadistaId.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador del brigadista es obligatorio"
            );
        }

        if (request == null
                || request.estado() == null
                || request.estado().isBlank()) {
            throw new IllegalArgumentException(
                    "El estado del brigadista es obligatorio"
            );
        }

        String nuevoEstado = request.estado().trim().toUpperCase();

        if (!nuevoEstado.equals("ACTIVO")
                && !nuevoEstado.equals("INACTIVO")) {
            throw new IllegalArgumentException(
                    "El estado debe ser ACTIVO o INACTIVO"
            );
        }

        Brigadista brigadista = brigadistaRepository
                .findByUsuarioIdExterno(brigadistaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el brigadista indicado"
                ));

        EstadoBrigadista estado = estadoBrigadistaRepository
                .findByNombre(nuevoEstado)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el estado " + nuevoEstado
                ));

        brigadista.setEstado(estado);

        brigadistaRepository.save(brigadista);

        return new ActivacionBrigadistaResponse(
                UUID.randomUUID(),
                brigadista.getUsuarioIdExterno(),
                nuevoEstado,
                OffsetDateTime.now()
        );
    }
}