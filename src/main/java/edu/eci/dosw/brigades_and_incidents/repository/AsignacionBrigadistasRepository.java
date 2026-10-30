
package edu.eci.dosw.brigades_and_incidents.repository;

import edu.eci.dosw.brigades_and_incidents.entity.AsignacionBrigadistas;
import edu.eci.dosw.brigades_and_incidents.enums.EstadoAsignacion;
import edu.eci.dosw.brigades_and_incidents.enums.TipoUbicacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AsignacionBrigadistasRepository
        extends JpaRepository<AsignacionBrigadistas, UUID> {

    List<AsignacionBrigadistas> findByEventoId(UUID eventoId);

    List<AsignacionBrigadistas> findByBrigadista_IdBrigadista(
            UUID brigadistaId);

    List<AsignacionBrigadistas> findByEstado(
            EstadoAsignacion estado);

    // Consulta si un brigadista tiene alguna asignación vigente.
    boolean existsByBrigadista_IdBrigadistaAndEstado(
            UUID brigadistaId,
            EstadoAsignacion estado);

    List<AsignacionBrigadistas>
            findByBrigadista_IdBrigadistaAndEventoIdAndUbicacionIdAndTipoUbicacionAndEstado(
                    UUID brigadistaId,
                    UUID eventoId,
                    UUID ubicacionId,
                    TipoUbicacion tipoUbicacion,
                    EstadoAsignacion estado);
}
