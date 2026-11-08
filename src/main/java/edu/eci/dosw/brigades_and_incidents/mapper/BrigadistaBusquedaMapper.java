package edu.eci.dosw.brigades_and_incidents.mapper;

import edu.eci.dosw.brigades_and_incidents.dto.AsignacionBrigadistaResumenResponse;
import edu.eci.dosw.brigades_and_incidents.entity.AsignacionBrigadista;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BrigadistaBusquedaMapper {

    public List<AsignacionBrigadistaResumenResponse> convertirAsignaciones(
            List<AsignacionBrigadista> asignaciones
    ) {
        return asignaciones.stream()
                .map(this::convertirAsignacion)
                .toList();
    }

    private AsignacionBrigadistaResumenResponse convertirAsignacion(
            AsignacionBrigadista asignacion
    ) {
        return new AsignacionBrigadistaResumenResponse(
                asignacion.getIdAsignacion(),
                asignacion.getEventoId(),
                asignacion.getUbicacionId(),
                asignacion.getTipoUbicacion().getNombre(),
                asignacion.getEstado().getNombre()
        );
    }
}