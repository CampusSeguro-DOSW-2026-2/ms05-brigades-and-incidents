package edu.eci.dosw.brigades_and_incidents.dto;

public record MetadatosPaginacionDTO(
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {
}
