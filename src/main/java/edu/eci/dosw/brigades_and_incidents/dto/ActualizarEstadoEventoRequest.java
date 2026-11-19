
package edu.eci.dosw.brigades_and_incidents.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

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

    public ActualizarEstadoEventoRequest {
        zonas = zonas == null ? List.of() : List.copyOf(zonas);
    }

    /**
     * Edificio o zona del alcance del evento. {@code tipo} es opcional (EDIFICIO, ZONA_SEGURA o PUNTO_ENCUENTRO);
     * si MS06 no lo envía, se toma como EDIFICIO.
     */
    public record ZonaEventoDTO(
            String zonaId,
            String nombre,
            @JsonAlias({"type", "tipoUbicacion"})
            String tipo
    ) {

        public ZonaEventoDTO(String zonaId, String nombre) {
            this(zonaId, nombre, null);
        }
    }
}
