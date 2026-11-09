package edu.eci.dosw.brigades_and_incidents.dto;

import java.util.List;

public record BuscarBrigadistaResponse(
        String idBrigadista,
        String nombre,
        String rol,
        String estado,
        List<AsignacionBrigadistaResumenResponse> asignaciones
) {
}