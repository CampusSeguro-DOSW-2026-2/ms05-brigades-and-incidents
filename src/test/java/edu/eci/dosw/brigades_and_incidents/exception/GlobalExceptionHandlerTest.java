package edu.eci.dosw.brigades_and_incidents.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @SuppressWarnings("unused")
    void metodoDummy(String valor) {
        // Solo sirve para construir un MethodParameter en las pruebas.
    }

    private void verificar(
            ResponseEntity<ErrorResponse> respuesta,
            int codigo,
            String mensaje
    ) {
        assertThat(respuesta.getStatusCode().value()).isEqualTo(codigo);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().codigo()).isEqualTo(codigo);
        assertThat(respuesta.getBody().mensaje()).isEqualTo(mensaje);
        assertThat(respuesta.getBody().fecha()).isNotNull();
        assertThat(respuesta.getBody().error()).isNotBlank();
    }

    @Test
    void recursoNoEncontradoDevuelve404() {
        verificar(
                handler.manejarNoEncontrado(
                        new RecursoNoEncontradoException("No existe")),
                404, "No existe");
    }

    @Test
    void conflictoDevuelve409() {
        verificar(
                handler.manejarConflicto(
                        new ConflictoOperacionException("Conflicto")),
                409, "Conflicto");
    }

    @Test
    void errorDeIntegracionMs01Devuelve502ConMensajeGenerico() {
        verificar(
                handler.manejarIntegracionMs01(
                        new Ms01IntegrationException("detalle interno")),
                502,
                "No fue posible completar la operación con el servicio de identidad.");
    }

    @Test
    void evidenciaQueNoExisteDevuelve404() {
        verificar(
                handler.manejarEvidenciaInvalida(
                        new EvidenciaIncidenteException("La evidencia no existe")),
                404, "La evidencia no existe");
    }

    @Test
    void evidenciaInvalidaDevuelve400() {
        verificar(
                handler.manejarEvidenciaInvalida(
                        new EvidenciaIncidenteException("Formato no permitido")),
                400, "Formato no permitido");
    }

    @Test
    void evidenciaSinMensajeDevuelve400() {
        ResponseEntity<ErrorResponse> respuesta =
                handler.manejarEvidenciaInvalida(
                        new EvidenciaIncidenteException(null));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void argumentoInvalidoDevuelve400() {
        verificar(
                handler.manejarArgumentoInvalido(
                        new IllegalArgumentException("Argumento inválido")),
                400, "Argumento inválido");
    }

    @Test
    void validacionesConcatenanLosErroresDeCampo() throws Exception {
        MethodParameter parametro = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod(
                        "metodoDummy", String.class),
                0);
        BeanPropertyBindingResult resultado =
                new BeanPropertyBindingResult(new Object(), "objeto");
        resultado.addError(
                new FieldError("objeto", "nombre", "no puede estar vacío"));
        resultado.addError(
                new FieldError("objeto", "edad", "debe ser positiva"));

        ResponseEntity<ErrorResponse> respuesta =
                handler.manejarValidaciones(
                        new MethodArgumentNotValidException(
                                parametro, resultado));

        verificar(respuesta, 400,
                "nombre: no puede estar vacío; edad: debe ser positiva");
    }

    @Test
    void estadoHttpConRazonUsaLaRazon() {
        verificar(
                handler.manejarEstadoHttp(
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN, "Prohibido")),
                403, "Prohibido");
    }

    @Test
    void estadoHttpSinRazonUsaLaFraseDelEstado() {
        verificar(
                handler.manejarEstadoHttp(
                        new ResponseStatusException(HttpStatus.FORBIDDEN)),
                403, "Forbidden");
    }

    @Test
    void rutaNoEncontradaDevuelve404() {
        verificar(
                handler.manejarRutaNoEncontrada(
                        mock(NoResourceFoundException.class),
                        new MockHttpServletRequest("GET", "/ruta/inexistente")),
                404, "La ruta solicitada no existe.");
    }

    @Test
    void errorGeneralDevuelve500SinExponerDetalles() {
        verificar(
                handler.manejarErrorGeneral(
                        new RuntimeException("secreto interno"),
                        new MockHttpServletRequest("POST", "/api/v1/x")),
                500, "Ocurrió un error interno al procesar la solicitud.");
    }
}