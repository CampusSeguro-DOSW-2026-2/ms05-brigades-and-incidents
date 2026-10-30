
package edu.eci.dosw.brigades_and_incidents.dto;

import java.util.List;
import java.util.UUID;

public record AsignacionPredeterminadaResponse(
        List<AsignacionPropuestaDTO> asignaciones,
        List<UUID> brigadistasSinAsignar,
        int totalDisponibles,
        int totalRequeridos,
        boolean distribucionAjustada
) {}
