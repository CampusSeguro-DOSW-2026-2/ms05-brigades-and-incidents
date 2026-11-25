package edu.eci.dosw.brigades_and_incidents.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

/**
 * Cuerpo de error de los Estándares de integración (§7.1): exactamente status, codigo, mensaje, errores y
 * correlation_id. El Gateway convierte en 502 cualquier error que no tenga esta forma.
 */
public record ErrorResponse(
        int status,
        String codigo,
        String mensaje,
        List<ErrorCampo> errores,
        @JsonProperty("correlation_id") String correlationId
) {

    public static final String CORRELATION_HEADER = "X-Correlation-ID";

    public ErrorResponse {
        errores = errores == null ? List.of() : List.copyOf(errores);
    }

    /** Error de un campo concreto (validaciones). */
    public record ErrorCampo(String campo, String mensaje) {
    }

    public static ErrorResponse de(HttpStatus status, String mensaje) {
        return de(status, mensaje, List.of());
    }

    public static ErrorResponse de(HttpStatus status, String mensaje, List<ErrorCampo> errores) {
        String texto = mensaje == null || mensaje.isBlank() ? status.getReasonPhrase() : mensaje;
        return new ErrorResponse(status.value(), codigoDe(status), texto, errores, correlationIdRecibido());
    }

    /** Código del catálogo de los Estándares (§7.2) para cada estado HTTP. */
    static String codigoDe(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "VALIDACION";
            case UNAUTHORIZED -> "NO_AUTENTICADO";
            case FORBIDDEN -> "PROHIBIDO";
            case NOT_FOUND -> "NO_ENCONTRADO";
            case METHOD_NOT_ALLOWED -> "METODO_NO_PERMITIDO";
            case CONFLICT -> "CONFLICTO";
            case UNSUPPORTED_MEDIA_TYPE -> "TIPO_NO_SOPORTADO";
            case UNPROCESSABLE_CONTENT -> "REGLA_NEGOCIO";
            case BAD_GATEWAY, SERVICE_UNAVAILABLE -> "SERVICIO_NO_DISPONIBLE";
            default -> "ERROR_INTERNO";
        };
    }

    /** El X-Correlation-ID recibido; si la solicitud no lo trae (p. ej. una prueba sin Gateway), uno nuevo. */
    private static String correlationIdRecibido() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos instanceof ServletRequestAttributes servlet) {
            String recibido = servlet.getRequest().getHeader(CORRELATION_HEADER);
            if (recibido != null && !recibido.isBlank()) {
                return recibido.trim();
            }
        }
        return UUID.randomUUID().toString();
    }
}
