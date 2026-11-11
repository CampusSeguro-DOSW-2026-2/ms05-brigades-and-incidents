
package edu.eci.dosw.brigades_and_incidents.dto;

import java.util.List;

public record PanelResponsabilidadesResponse(
        List<AsignacionBrigadistaResumenResponse> asignaciones,
        List<IncidenteResumenDTO> tareasPendientes,
        MetadatosPaginacionDTO metadatosPaginacion
) {
    public PanelResponsabilidadesResponse {
        asignaciones = asignaciones == null
                ? List.of()
                : List.copyOf(asignaciones);

        tareasPendientes = tareasPendientes == null
                ? List.of()
                : List.copyOf(tareasPendientes);
    }
}
