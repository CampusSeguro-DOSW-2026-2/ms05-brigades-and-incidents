package edu.eci.dosw.brigades_and_incidents.repository;

import edu.eci.dosw.brigades_and_incidents.entity.EstadoBrigadista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoBrigadistaRepository
        extends JpaRepository<EstadoBrigadista, Long> {

    Optional<EstadoBrigadista> findByNombre(String nombre);
}