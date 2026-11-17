package edu.eci.dosw.brigades_and_incidents.dto;

/** Evento abierto a la auto asignación junto con la asignación vigente del brigadista en él (si tiene). */
public record AutoasignacionEventoDTO(
        EventoOperacionDTO evento,
        String idAsignacion,
        String ubicacionId,
        String nombreUbicacion
) {
}
