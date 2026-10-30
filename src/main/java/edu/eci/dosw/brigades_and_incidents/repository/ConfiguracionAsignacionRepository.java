
package edu.eci.dosw.brigades_and_incidents.repository;

import edu.eci.dosw.brigades_and_incidents.entity.ConfiguracionAsignacion;
import edu.eci.dosw.brigades_and_incidents.enums.TipoUbicacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConfiguracionAsignacionRepository
        extends JpaRepository<ConfiguracionAsignacion, UUID> {

    // Consultar la configuración de un tipo de ubicación.
    Optional<ConfiguracionAsignacion> findByTipoUbicacion(
            TipoUbicacion tipoUbicacion
    );

    // Verificar si ya existe una configuración para ese tipo.
    boolean existsByTipoUbicacion(TipoUbicacion tipoUbicacion);
}
