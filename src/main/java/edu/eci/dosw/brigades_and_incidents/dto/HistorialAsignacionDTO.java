
package edu.eci.dosw.brigades_and_incidents.dto;

import edu.eci.dosw.brigades_and_incidents.enums.TipoUbicacion;
import java.time.LocalDateTime;
import java.util.UUID;

public record HistorialAsignacionDTO(
        UUID idHistorial,
        UUID asignacionId,
        UUID ubicacionAnteriorId,
        TipoUbicacion tipoUbicacionAnterior,
        UUID ubicacionNuevaId,
        TipoUbicacion tipoUbicacionNueva,
        LocalDateTime fechaCambio
) {}
