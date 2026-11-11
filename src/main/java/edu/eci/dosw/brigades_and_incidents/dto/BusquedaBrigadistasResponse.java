
package edu.eci.dosw.brigades_and_incidents.dto;

import java.util.List;

public record BusquedaBrigadistasResponse(
        String mensaje,
        List<BuscarBrigadistaResponse> resultados
) {
    public BusquedaBrigadistasResponse {
        resultados = resultados == null
                ? List.of()
                : List.copyOf(resultados);
    }
}
