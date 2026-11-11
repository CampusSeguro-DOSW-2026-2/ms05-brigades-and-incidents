
package edu.eci.dosw.brigades_and_incidents.repository;

import edu.eci.dosw.brigades_and_incidents.entity.TipoUbicacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoUbicacionRepository
        extends JpaRepository<TipoUbicacion, Long> {

    Optional<TipoUbicacion> findByNombre(String nombre);
}
