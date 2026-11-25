package edu.eci.dosw.brigades_and_incidents.dto;

import java.util.List;

/** Vista de auto asignación del brigadista: su estado y los eventos en los que puede elegir edificio. */
public record AutoasignacionResponse(
        String brigadistaId,
        String estado,
        List<AutoasignacionEventoDTO> eventos
) {

    public AutoasignacionResponse {
        eventos = eventos == null ? List.of() : List.copyOf(eventos);
    }
}
