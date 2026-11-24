
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
        List<ZonaEventoDTO> zonas,
        /**
         * Edificios del alcance que MS06 envía aparte de {@code zonas} (solo con su id). Se suman al alcance como
         * ubicaciones tipo EDIFICIO; si un id ya viene en {@code zonas}, se conserva el de {@code zonas}.
         */
        List<String> edificios

) {

    public ActualizarEstadoEventoRequest {
        zonas = zonas == null ? List.of() : List.copyOf(zonas);
        edificios = edificios == null ? List.of() : List.copyOf(edificios);
    }

    /**
     * Zona del alcance del evento. {@code tipo} es opcional (EDIFICIO, ZONA_SEGURA o PUNTO_ENCUENTRO); MS06 manda en
     * esta lista solo su alcance ZONE y sin tipo, por eso el valor por defecto es ZONA_SEGURA. Los edificios llegan
     * aparte, en {@code edificios}.
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
