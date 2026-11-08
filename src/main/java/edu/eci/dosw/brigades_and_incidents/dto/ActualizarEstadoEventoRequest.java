package edu.eci.dosw.brigades_and_incidents.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

public record ActualizarEstadoEventoRequest(

        String eventoId,

        String nombre,

        String tipoEvento,

        boolean simulacro,

        String estado,

        OffsetDateTime fechaInicio,

        OffsetDateTime fechaFin,

        List<ZonaEventoDTO> zonas

) {

    public record ZonaEventoDTO(

            String zonaId,

            String nombre

    ) {
    }
}