package edu.eci.dosw.brigades_and_incidents.repository;

import edu.eci.dosw.brigades_and_incidents.entity.HistorialAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HistorialAsignacionRepository
        extends JpaRepository<HistorialAsignacion, UUID> {

    List<HistorialAsignacion> findByAsignacion_IdAsignacionOrderByFechaCambioAsc(
            UUID asignacionId);
}