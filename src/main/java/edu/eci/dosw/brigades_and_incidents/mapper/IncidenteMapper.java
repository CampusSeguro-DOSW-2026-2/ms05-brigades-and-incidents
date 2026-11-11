package edu.eci.dosw.brigades_and_incidents.mapper;

import edu.eci.dosw.brigades_and_incidents.dto.IncidenteResponse;
import edu.eci.dosw.brigades_and_incidents.entity.Incidente;
import org.springframework.stereotype.Component;

@Component
public class IncidenteMapper {

    public IncidenteResponse toResponse(Incidente incidente) {

        return new IncidenteResponse(
                incidente.getId(),
                incidente.getEventoId(),
                incidente.getNombreAfectado(),
                incidente.getReportanteId(),
                incidente.getUbicacionId(),
                incidente.getTipoUbicacion().getNombre(),
                incidente.getPrioridad().getNombre(),
                incidente.getEstado().getNombre(),
                incidente.getDescripcion(),
                incidente.getFechaReporte()
        );
    }
}