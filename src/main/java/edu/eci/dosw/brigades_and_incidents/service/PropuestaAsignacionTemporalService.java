
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.dto.AsignacionPredeterminadaRequest;
import edu.eci.dosw.brigades_and_incidents.dto.AsignacionPropuestaDTO;
import edu.eci.dosw.brigades_and_incidents.exception.ConflictoOperacionException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PropuestaAsignacionTemporalService {

    private final Map<UUID, PropuestaTemporal> propuestas =
            new ConcurrentHashMap<>();

    public void guardar(
            UUID eventoId,
            List<AsignacionPropuestaDTO> asignaciones,
            List<AsignacionPredeterminadaRequest.UbicacionSeleccionada> ubicaciones) {

        if (eventoId == null) {
            throw new IllegalArgumentException(
                    "El ID del evento es obligatorio");
        }

        if (asignaciones == null || ubicaciones == null) {
            throw new IllegalArgumentException(
                    "Las asignaciones y ubicaciones son obligatorias");
        }

        PropuestaTemporal propuesta = new PropuestaTemporal(
                List.copyOf(asignaciones),
                List.copyOf(ubicaciones)
        );

        propuestas.put(eventoId, propuesta);
    }

    public List<AsignacionPropuestaDTO> consultar(UUID eventoId) {
        return consultarPropuesta(eventoId).asignaciones();
    }

    public List<AsignacionPredeterminadaRequest.UbicacionSeleccionada>
            consultarUbicaciones(UUID eventoId) {

        return consultarPropuesta(eventoId).ubicaciones();
    }

    private PropuestaTemporal consultarPropuesta(UUID eventoId) {

        if (eventoId == null) {
            throw new IllegalArgumentException(
                    "El ID del evento es obligatorio");
        }

        PropuestaTemporal propuesta = propuestas.get(eventoId);

        if (propuesta == null) {
            throw new ConflictoOperacionException(
                    "No existe una propuesta pendiente para este evento. "
                            + "Debe generarse una nueva propuesta");
        }

        return propuesta;
    }

    public void eliminar(UUID eventoId) {

        if (eventoId == null) {
            throw new IllegalArgumentException(
                    "El ID del evento es obligatorio");
        }

        propuestas.remove(eventoId);
    }

    private record PropuestaTemporal(
            List<AsignacionPropuestaDTO> asignaciones,
            List<AsignacionPredeterminadaRequest.UbicacionSeleccionada> ubicaciones
    ) {}
}
