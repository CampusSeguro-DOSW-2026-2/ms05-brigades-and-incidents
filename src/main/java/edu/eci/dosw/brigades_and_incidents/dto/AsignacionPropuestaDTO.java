
package edu.eci.dosw.brigades_and_incidents.dto;

import edu.eci.dosw.brigades_and_incidents.enums.TipoAsignacion;
import edu.eci.dosw.brigades_and_incidents.enums.TipoUbicacion;

import java.util.UUID;

public record AsignacionPropuestaDTO(
        UUID brigadistaId,
        UUID eventoId,
        UUID ubicacionId,
        TipoUbicacion tipoUbicacion,
        TipoAsignacion tipoAsignacion
) {
}
