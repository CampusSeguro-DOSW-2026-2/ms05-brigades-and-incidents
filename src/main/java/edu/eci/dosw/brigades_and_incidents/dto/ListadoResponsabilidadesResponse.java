package edu.eci.dosw.brigades_and_incidents.dto;

import java.util.List;

public record ListadoResponsabilidadesResponse(
        List<AsignacionResponse> asignaciones,
        MetadatosPaginacionDTO metadatosPaginacion
) {
}
