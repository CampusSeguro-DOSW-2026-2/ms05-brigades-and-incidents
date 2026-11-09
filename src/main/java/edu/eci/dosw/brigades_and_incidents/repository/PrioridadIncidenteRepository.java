package edu.eci.dosw.brigades_and_incidents.repository;

import edu.eci.dosw.brigades_and_incidents.entity.PrioridadIncidente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrioridadIncidenteRepository
        extends JpaRepository<PrioridadIncidente, Long> {

    Optional<PrioridadIncidente> findByNombre(String nombre);
}