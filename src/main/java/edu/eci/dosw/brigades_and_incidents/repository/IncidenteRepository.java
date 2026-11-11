
package edu.eci.dosw.brigades_and_incidents.repository;

import edu.eci.dosw.brigades_and_incidents.entity.Incidente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidenteRepository extends
        JpaRepository<Incidente, UUID>,
        JpaSpecificationExecutor<Incidente> {

    List<Incidente> findByEventoId(String eventoId);

    List<Incidente> findByReportanteId(String reportanteId);

    List<Incidente> findByEventoIdAndUbicacionId(
            String eventoId,
            String ubicacionId
    );

    Optional<Incidente> findByIdAndEventoId(
            UUID id,
            String eventoId
    );

    Optional<Incidente> findByIdAndEventoIdAndEliminadoFalse(
            UUID id,
            String eventoId
    );
}
