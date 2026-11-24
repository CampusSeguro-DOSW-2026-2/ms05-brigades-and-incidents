package edu.eci.dosw.brigades_and_incidents.exception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** ErrorResponse de los Estándares (§7.1 y §7.2): cinco campos, código del catálogo y correlation_id recibido. */
class ErrorResponseEstandarTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void limpiar() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static void verificar(ResponseEntity<ErrorResponse> respuesta, int status, String codigo) {
        assertThat(respuesta.getStatusCode().value()).isEqualTo(status);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().status()).isEqualTo(status);
        assertThat(respuesta.getBody().codigo()).isEqualTo(codigo);
        assertThat(respuesta.getBody().errores()).isEmpty();
    }

    private static MethodParameter parametro() throws NoSuchMethodException {
        return new MethodParameter(ErrorResponseEstandarTest.class.getDeclaredMethod("metodo", String.class), 0);
    }

    @SuppressWarnings("unused")
    private void metodo(String valor) {
        // Solo para construir el MethodParameter de las excepciones de cabecera.
    }

    @Test
    void elCorrelationIdEsElRecibido() {
        MockHttpServletRequest solicitud = new MockHttpServletRequest();
        solicitud.addHeader(ErrorResponse.CORRELATION_HEADER, "3f6c2a9e-8b1d-4c7a-9e2f-1a5b7c9d0e42");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(solicitud));

        ErrorResponse error = ErrorResponse.de(HttpStatus.NOT_FOUND, "No existe.");

        assertThat(error.correlationId()).isEqualTo("3f6c2a9e-8b1d-4c7a-9e2f-1a5b7c9d0e42");
        assertThat(error.codigo()).isEqualTo("NO_ENCONTRADO");
    }

    @Test
    void sinSolicitudSeGeneraUnCorrelationIdYElMensajePorDefecto() {
        ErrorResponse error = ErrorResponse.de(HttpStatus.CONFLICT, " ");

        assertThat(error.correlationId()).isNotBlank();
        assertThat(error.mensaje()).isEqualTo(HttpStatus.CONFLICT.getReasonPhrase());
        assertThat(error.codigo()).isEqualTo("CONFLICTO");
    }

    @Test
    void cadaEstadoTieneSuCodigoDelCatalogo() {
        assertThat(ErrorResponse.codigoDe(HttpStatus.BAD_REQUEST)).isEqualTo("VALIDACION");
        assertThat(ErrorResponse.codigoDe(HttpStatus.UNAUTHORIZED)).isEqualTo("NO_AUTENTICADO");
        assertThat(ErrorResponse.codigoDe(HttpStatus.FORBIDDEN)).isEqualTo("PROHIBIDO");
        assertThat(ErrorResponse.codigoDe(HttpStatus.METHOD_NOT_ALLOWED)).isEqualTo("METODO_NO_PERMITIDO");
        assertThat(ErrorResponse.codigoDe(HttpStatus.UNSUPPORTED_MEDIA_TYPE)).isEqualTo("TIPO_NO_SOPORTADO");
        assertThat(ErrorResponse.codigoDe(HttpStatus.UNPROCESSABLE_CONTENT)).isEqualTo("REGLA_NEGOCIO");
        assertThat(ErrorResponse.codigoDe(HttpStatus.BAD_GATEWAY)).isEqualTo("SERVICIO_NO_DISPONIBLE");
        assertThat(ErrorResponse.codigoDe(HttpStatus.SERVICE_UNAVAILABLE)).isEqualTo("SERVICIO_NO_DISPONIBLE");
        assertThat(ErrorResponse.codigoDe(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo("ERROR_INTERNO");
    }

    @Test
    void sinIdentidadDelGatewayEs401YOtraCabeceraEs400() throws NoSuchMethodException {
        verificar(handler.manejarCabeceraFaltante(new MissingRequestHeaderException("X-User-Id", parametro())),
                401, "NO_AUTENTICADO");
        verificar(handler.manejarCabeceraFaltante(new MissingRequestHeaderException("X-Otra", parametro())),
                400, "VALIDACION");
    }

    @Test
    void accesoDenegadoEs403() {
        verificar(handler.manejarAccesoDenegado(new AccessDeniedException("no")), 403, "PROHIBIDO");
    }

    @Test
    void solicitudMalFormadaEs400() {
        verificar(handler.manejarSolicitudMalFormada(
                new MissingServletRequestParameterException("pagina", "int")), 400, "VALIDACION");
    }

    @Test
    void metodoNoPermitidoEs405YTipoNoSoportadoEs415() {
        verificar(handler.manejarMetodoNoPermitido(new HttpRequestMethodNotSupportedException("TRACE")),
                405, "METODO_NO_PERMITIDO");
        verificar(handler.manejarTipoNoSoportado(new HttpMediaTypeNotSupportedException(
                MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON))), 415, "TIPO_NO_SOPORTADO");
    }
}
