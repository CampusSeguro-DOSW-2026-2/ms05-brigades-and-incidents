package edu.eci.dosw.brigades_and_incidents.dto;

/** Edificio del alcance de un evento con el número de brigadistas que tiene asignados en este momento. */
public record EdificioEventoDTO(
        String ubicacionId,
        String nombre,
        String tipoUbicacion,
        long brigadistasAsignados
) {
}
