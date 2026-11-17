package edu.eci.dosw.brigades_and_incidents.dto;

/**
 * Brigadista en el panel de gestión del coordinador: su estado, su edificio en el evento (si tiene) y los
 * incidentes que está atendiendo y que ha resuelto en ese evento.
 */
public record BrigadistaGestionDTO(
        String brigadistaId,
        String nombre,
        String estado,
        String idAsignacion,
        String ubicacionId,
        String nombreUbicacion,
        long incidentesEnAtencion,
        long incidentesResueltos
) {
}
