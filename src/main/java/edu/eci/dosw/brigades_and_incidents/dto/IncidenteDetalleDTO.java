
package edu.eci.dosw.brigades_and_incidents.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record IncidenteDetalleDTO(

        UUID idIncidente,

        String eventoId,

        String nombreAfectado,

        String ubicacionId,

        String tipoUbicacion,

        String descripcion,

        String prioridad,

        String estado,

        String reportanteId,

        String brigadistaResponsableId,

        OffsetDateTime fechaReporte,

        OffsetDateTime fechaPrimeraAtencion,

        OffsetDateTime fechaResolucion,

        String observacionesResolucion,

        OffsetDateTime fechaCierre,

        List<EvidenciaIncidenteDTO> evidencias

) {
}
