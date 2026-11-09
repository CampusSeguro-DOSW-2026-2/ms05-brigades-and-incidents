
package edu.eci.dosw.brigades_and_incidents.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

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

        String mensaje = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + ": " + error.getDefaultMessage()
                )
                .collect(Collectors.joining("; "));

        return construirRespuesta(
                HttpStatus.BAD_REQUEST,
                mensaje
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

        ErrorResponse respuesta = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensaje
        );

        return ResponseEntity
                .status(status)
                .body(respuesta);
    }
}
