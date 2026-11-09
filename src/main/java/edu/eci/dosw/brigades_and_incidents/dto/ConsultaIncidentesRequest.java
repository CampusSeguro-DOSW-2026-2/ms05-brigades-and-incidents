package edu.eci.dosw.brigades_and_incidents.dto;

public record ConsultaIncidentesRequest(
        String prioridad,
        String estado,
        String ubicacionId,
        String ordenarPor,
        String direccion,
        Integer pagina,
        Integer tamanio
) {
}