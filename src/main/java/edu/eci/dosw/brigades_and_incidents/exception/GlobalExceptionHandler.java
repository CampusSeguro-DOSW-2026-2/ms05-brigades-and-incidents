
package edu.eci.dosw.brigades_and_incidents.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Recurso solicitado que no existe: HTTP 404.
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(
            RecursoNoEncontradoException ex) {

        return construirRespuesta(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
    }

    // Operación que incumple una regla de negocio: HTTP 409.
    @ExceptionHandler(ConflictoOperacionException.class)
    public ResponseEntity<ErrorResponse> manejarConflicto(
            ConflictoOperacionException ex) {

        return construirRespuesta(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
    }

    // Argumentos inválidos: HTTP 400.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarArgumentoInvalido(
            IllegalArgumentException ex) {

        return construirRespuesta(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    // Errores de validación de los DTO: HTTP 400.
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

    // Errores HTTP generados desde los controladores: 403, 401, etc.
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

    // Cualquier error inesperado: HTTP 500.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorGeneral(
            Exception ex) {

        return construirRespuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno al procesar la solicitud."
        );
    }

    // Construye todas las respuestas con el mismo formato.
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
