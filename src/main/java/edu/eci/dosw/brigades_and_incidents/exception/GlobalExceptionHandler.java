
package edu.eci.dosw.brigades_and_incidents.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(
            RecursoNoEncontradoException ex) {

        return construirRespuesta(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
    }

    @ExceptionHandler(ConflictoOperacionException.class)
    public ResponseEntity<ErrorResponse> manejarConflicto(
            ConflictoOperacionException ex) {

        return construirRespuesta(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
    }

    @ExceptionHandler(Ms01IntegrationException.class)
    public ResponseEntity<ErrorResponse> manejarIntegracionMs01(
            Ms01IntegrationException ex) {

        logger.warn("Error de integración con MS01: {}", ex.getMessage());

        return construirRespuesta(
                HttpStatus.BAD_GATEWAY,
                "No fue posible completar la operación con el servicio de identidad."
        );
    }

    @ExceptionHandler(EvidenciaIncidenteException.class)
    public ResponseEntity<ErrorResponse> manejarEvidenciaInvalida(
            EvidenciaIncidenteException ex) {

        HttpStatus status = ex.getMessage() != null
                && ex.getMessage().toLowerCase().contains("no existe")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;

        return construirRespuesta(status, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarArgumentoInvalido(
            IllegalArgumentException ex) {

        return construirRespuesta(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidaciones(
            MethodArgumentNotValidException ex) {

        List<ErrorResponse.ErrorCampo> errores = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.ErrorCampo(
                        error.getField(), error.getDefaultMessage()))
                .toList();

        return construirRespuesta(
                HttpStatus.BAD_REQUEST,
                "Los datos enviados no son válidos.",
                errores
        );
    }

    // Identidad del Gateway ausente: 401 (Estándares §4); cualquier otra cabecera obligatoria: 400.
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> manejarCabeceraFaltante(
            MissingRequestHeaderException ex) {

        if (ex.getHeaderName().toLowerCase(Locale.ROOT).startsWith("x-user-")) {
            return construirRespuesta(HttpStatus.UNAUTHORIZED, "Se requiere autenticación.");
        }
        return construirRespuesta(
                HttpStatus.BAD_REQUEST,
                "Falta la cabecera " + ex.getHeaderName() + "."
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> manejarAccesoDenegado(
            AccessDeniedException ex) {

        return construirRespuesta(
                HttpStatus.FORBIDDEN,
                "No tiene permisos para realizar esta operación."
        );
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> manejarSolicitudMalFormada(
            Exception ex) {

        return construirRespuesta(
                HttpStatus.BAD_REQUEST,
                "La solicitud tiene un formato inválido."
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> manejarMetodoNoPermitido(
            HttpRequestMethodNotSupportedException ex) {

        return construirRespuesta(
                HttpStatus.METHOD_NOT_ALLOWED,
                "El método no está permitido en esta ruta."
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> manejarTipoNoSoportado(
            HttpMediaTypeNotSupportedException ex) {

        return construirRespuesta(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "El tipo de contenido no está soportado."
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> manejarEstadoHttp(
            ResponseStatusException ex) {

        HttpStatus status = HttpStatus.valueOf(
                ex.getStatusCode().value()
        );

        return construirRespuesta(
                status,
                ex.getReason() != null
                        ? ex.getReason()
                        : status.getReasonPhrase()
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> manejarRutaNoEncontrada(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        logger.debug(
                "Ruta no encontrada: {} {}",
                request.getMethod(),
                request.getRequestURI()
        );

        return construirRespuesta(
                HttpStatus.NOT_FOUND,
                "La ruta solicitada no existe."
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorGeneral(
            Exception ex,
            HttpServletRequest request) {

        logger.error(
                "Error interno al procesar {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex
        );

        return construirRespuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno al procesar la solicitud."
        );
    }

    private ResponseEntity<ErrorResponse> construirRespuesta(
            HttpStatus status,
            String mensaje) {

        return construirRespuesta(status, mensaje, List.of());
    }

    private ResponseEntity<ErrorResponse> construirRespuesta(
            HttpStatus status,
            String mensaje,
            List<ErrorResponse.ErrorCampo> errores) {

        return ResponseEntity
                .status(status)
                .body(ErrorResponse.de(status, mensaje, errores));
    }
}
