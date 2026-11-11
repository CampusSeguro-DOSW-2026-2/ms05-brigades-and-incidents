
package edu.eci.dosw.brigades_and_incidents.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        LocalDateTime fecha,
        int codigo,
        String error,
        String mensaje
) {
}
