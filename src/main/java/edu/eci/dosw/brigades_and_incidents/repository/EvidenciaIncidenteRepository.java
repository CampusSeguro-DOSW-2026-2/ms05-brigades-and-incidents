package edu.eci.dosw.brigades_and_incidents.repository;

import edu.eci.dosw.brigades_and_incidents.entity.EvidenciaIncidente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EvidenciaIncidenteRepository
        extends JpaRepository<EvidenciaIncidente, UUID> {

    List<EvidenciaIncidente> findByIncidenteId(UUID incidenteId);

    long countByIncidenteId(UUID incidenteId);
}