package edu.eci.dosw.brigades_and_incidents.dto;

import java.util.List;

/**
 * Panel de gestión de brigadistas de un evento. La lista viene ordenada: primero los activos, agrupados por el
 * nombre del edificio asignado (orden alfabético) y luego por nombre; después los inactivos por nombre.
 */
public record GestionBrigadistasEventoResponse(
        EventoOperacionDTO evento,
        List<BrigadistaGestionDTO> brigadistas
) {

    public GestionBrigadistasEventoResponse {
        brigadistas = brigadistas == null ? List.of() : List.copyOf(brigadistas);
    }
}
