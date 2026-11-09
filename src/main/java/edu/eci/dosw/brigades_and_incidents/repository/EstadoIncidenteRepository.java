package edu.eci.dosw.brigades_and_incidents.repository;

import edu.eci.dosw.brigades_and_incidents.entity.EstadoIncidente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoIncidenteRepository
        extends JpaRepository<EstadoIncidente, Long> {

    Optional<EstadoIncidente> findByNombre(String nombre);
}