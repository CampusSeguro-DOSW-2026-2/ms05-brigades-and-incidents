
package edu.eci.dosw.brigades_and_incidents.repository;

import edu.eci.dosw.brigades_and_incidents.entity.EstadoAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoAsignacionRepository
        extends JpaRepository<EstadoAsignacion, Long> {

    Optional<EstadoAsignacion> findByNombre(String nombre);
}
