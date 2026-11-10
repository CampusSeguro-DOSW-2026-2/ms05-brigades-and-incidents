package edu.eci.dosw.brigades_and_incidents.dto;

import java.util.List;

public record PanelResponsabilidadesResponse(
        List<AsignacionBrigadistaResumenResponse> asignaciones,
        List<IncidenteResumenDTO> tareasPendientes,
        MetadatosPaginacionDTO metadatosPaginacion
) {
}
