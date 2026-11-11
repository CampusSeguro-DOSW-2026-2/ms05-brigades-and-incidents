package edu.eci.dosw.brigades_and_incidents.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Página de una colección con el formato de los Estándares de integración (§9):
 * contenido, page, size, total_elementos y total_paginas.
 */
public record PaginaResponse<T>(
        List<T> contenido,
        int page,
        int size,
        @JsonProperty("total_elementos") long totalElementos,
        @JsonProperty("total_paginas") int totalPaginas
) {

    public PaginaResponse {
        contenido = contenido == null ? List.of() : List.copyOf(contenido);
    }

    public static <T> PaginaResponse<T> de(Page<T> pagina) {
        return new PaginaResponse<>(
                pagina.getContent(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages()
        );
    }
}
