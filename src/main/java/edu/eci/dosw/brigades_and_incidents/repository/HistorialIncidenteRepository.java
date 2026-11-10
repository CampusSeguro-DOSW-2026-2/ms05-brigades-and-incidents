package edu.eci.dosw.brigades_and_incidents.repository;

import edu.eci.dosw.brigades_and_incidents.entity.HistorialIncidente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HistorialIncidenteRepository
        extends JpaRepository<HistorialIncidente, UUID> {

    List<HistorialIncidente> findByIncidente_IdOrderByFechaCambioAsc(
            UUID incidenteId
    );
}
