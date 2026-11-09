package edu.eci.dosw.brigades_and_incidents.repository;

import edu.eci.dosw.brigades_and_incidents.entity.AsignacionBrigadista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AsignacionBrigadistaRepository
        extends JpaRepository<AsignacionBrigadista, UUID> {

    boolean existsByBrigadistaIdAndEventoIdAndUbicacionIdAndTipoUbicacion_NombreAndEstado_Nombre(
            String brigadistaId,
            String eventoId,
            String ubicacionId,
            String tipoUbicacion,
            String estado
    );

    boolean existsByBrigadistaIdAndEventoIdAndUbicacionIdAndTipoUbicacion_NombreAndEstado_NombreAndIdAsignacionNot(
            String brigadistaId,
            String eventoId,
            String ubicacionId,
            String tipoUbicacion,
            String estado,
            UUID idAsignacion
    );

    List<AsignacionBrigadista> findByEventoId(String eventoId);

    List<AsignacionBrigadista> findByBrigadistaId(String brigadistaId);

    List<AsignacionBrigadista> findByEventoIdAndEstado_Nombre(
            String eventoId,
            String estado
    );

    List<AsignacionBrigadista> findByBrigadistaIdAndEventoId(
            String brigadistaId,
            String eventoId
    );

    List<AsignacionBrigadista> findByBrigadistaIdAndEstado_Nombre(
            String brigadistaId,
            String estado
    );
}