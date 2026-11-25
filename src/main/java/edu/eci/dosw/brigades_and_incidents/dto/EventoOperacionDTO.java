package edu.eci.dosw.brigades_and_incidents.dto;

import java.util.List;

/**
 * Evento abierto para la operación de brigadistas (programado o en ejecución), con sus edificios.
 * {@code permiteReasignar} indica si el coordinador puede cambiar asignaciones: solo en eventos reales.
 */
public record EventoOperacionDTO(
        String eventoId,
        String nombre,
        boolean simulacro,
        String estado,
        boolean permiteReasignar,
        List<EdificioEventoDTO> edificios
) {

    public EventoOperacionDTO {
        edificios = edificios == null ? List.of() : List.copyOf(edificios);
    }
}
