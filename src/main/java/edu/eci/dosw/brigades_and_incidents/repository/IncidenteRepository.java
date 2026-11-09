package edu.eci.dosw.brigades_and_incidents.repository;

import edu.eci.dosw.brigades_and_incidents.entity.Incidente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IncidenteRepository extends JpaRepository<Incidente, UUID> {

    List<Incidente> findByEventoId(String eventoId);

    List<Incidente> findByReportanteId(String reportanteId);

    List<Incidente> findByEventoIdAndUbicacionId(
            String eventoId,
            String ubicacionId
    );

}