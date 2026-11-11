
package edu.eci.dosw.brigades_and_incidents.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class UbicacionClientMs02Test {

    private HttpServer server;
    private UbicacionClientMs02 client;

    private static final String EDIFICIO_ID =
            "11111111-1111-1111-1111-111111111111";

    private static final String ZONA_ID =
            "22222222-2222-2222-2222-222222222222";

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(
                new InetSocketAddress("localhost", 0), 0
        );

        server.start();

        String baseUrl = "http://localhost:"
                + server.getAddress().getPort();

        client = new UbicacionClientMs02(
                RestClient.builder(),
                baseUrl,
                "clave-de-prueba-de-ms05-con-mas-de-32-caracteres"
        );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void responder(int status, String body) {
        server.createContext("/", exchange ->
                enviarRespuesta(exchange, status, body)
        );
    }

    private void enviarRespuesta(
            HttpExchange exchange,
            int status,
            String body
    ) throws IOException {

        byte[] respuesta = body == null
                ? new byte[0]
                : body.getBytes(StandardCharsets.UTF_8);

        if (status == 204) {
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.getResponseHeaders().add(
                    "Content-Type", "application/json"
            );
            exchange.sendResponseHeaders(
                    status, respuesta.length
            );

            if (respuesta.length > 0) {
                exchange.getResponseBody().write(respuesta);
            }
        }

        exchange.close();
    }

    @Test
    void debeConsultarEdificioActivo() {
        responder(200, """
                {
                    "id": "%s",
                    "nombre": "Bloque A",
                    "estado": "ACTIVO"
                }
                """.formatted(EDIFICIO_ID));

        UbicacionClient.UbicacionExterna resultado = client.consultarUbicacion(
                EDIFICIO_ID, "EDIFICIO"
        );

        assertNotNull(resultado);
    }

    @Test
    void debeConsultarEdificioInactivo() {
        responder(200, """
                {
                    "id": "%s",
                    "nombre": "Bloque B",
                    "estado": "INACTIVO"
                }
                """.formatted(EDIFICIO_ID));

        UbicacionClient.UbicacionExterna resultado = client.consultarUbicacion(
                EDIFICIO_ID, "EDIFICIO"
        );

        assertNotNull(resultado);
    }

    @Test
    void debeAceptarTipoDeUbicacionConEspaciosYMinusculas() {
        responder(200, """
                {
                    "id": "%s",
                    "nombre": "Bloque A",
                    "estado": "ACTIVO"
                }
                """.formatted(EDIFICIO_ID));

        UbicacionClient.UbicacionExterna resultado = client.consultarUbicacion(
                EDIFICIO_ID, " edificio "
        );

        assertNotNull(resultado);
    }

    @Test
    void debeConsultarZonaSegura() {
        responder(200, """
                {
                    "id": "%s",
                    "nombre": "Zona segura norte",
                    "estado": "ACTIVO",
                    "puntoEncuentro": false
                }
                """.formatted(ZONA_ID));

        UbicacionClient.UbicacionExterna resultado = client.consultarUbicacion(
                ZONA_ID, "ZONA_SEGURA"
        );

        assertNotNull(resultado);
    }

    @Test
    void debeConsultarPuntoDeEncuentro() {
        responder(200, """
                {
                    "id": "%s",
                    "nombre": "Punto principal",
                    "estado": "ACTIVO",
                    "puntoEncuentro": true
                }
                """.formatted(ZONA_ID));

        UbicacionClient.UbicacionExterna resultado = client.consultarUbicacion(
                ZONA_ID, "PUNTO_ENCUENTRO"
        );

        assertNotNull(resultado);
    }

    @Test
    void debeRetornarNullCuandoLaZonaNoEsPuntoDeEncuentro() {
        responder(200, """
                {
                    "id": "%s",
                    "nombre": "Zona segura norte",
                    "estado": "ACTIVO",
                    "puntoEncuentro": false
                }
                """.formatted(ZONA_ID));

        UbicacionClient.UbicacionExterna resultado = client.consultarUbicacion(
                ZONA_ID, "PUNTO_ENCUENTRO"
        );

        assertNull(resultado);
    }

    @Test
    void debeRetornarNullCuandoElPuntoNoEsZonaSegura() {
        responder(200, """
                {
                    "id": "%s",
                    "nombre": "Punto principal",
                    "estado": "ACTIVO",
                    "puntoEncuentro": true
                }
                """.formatted(ZONA_ID));

        UbicacionClient.UbicacionExterna resultado = client.consultarUbicacion(
                ZONA_ID, "ZONA_SEGURA"
        );

        assertNull(resultado);
    }

    @Test
    void debeRetornarNullCuandoMs02RespondeNotFound() {
        responder(404, """
                {
                    "mensaje": "Ubicación no encontrada"
                }
                """);

        UbicacionClient.UbicacionExterna resultado = client.consultarUbicacion(
                EDIFICIO_ID, "EDIFICIO"
        );

        assertNull(resultado);
    }

    @Test
    void debeRetornarNullCuandoLaRespuestaEstaVacia() {
        responder(204, null);

        UbicacionClient.UbicacionExterna resultado = client.consultarUbicacion(
                EDIFICIO_ID, "EDIFICIO"
        );

        assertNull(resultado);
    }

    @Test
    void debeRechazarIdentificadorNulo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> client.consultarUbicacion(null, "EDIFICIO")
        );
    }

    @Test
    void debeRechazarIdentificadorVacio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> client.consultarUbicacion("", "EDIFICIO")
        );
    }

    @Test
    void debeRechazarIdentificadorConEspacios() {
        assertThrows(
                IllegalArgumentException.class,
                () -> client.consultarUbicacion("   ", "EDIFICIO")
        );
    }

    @Test
    void debeRechazarTipoNulo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> client.consultarUbicacion(EDIFICIO_ID, null)
        );
    }

    @Test
    void debeRechazarTipoVacio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> client.consultarUbicacion(EDIFICIO_ID, "")
        );
    }

    @Test
    void debeRechazarTipoConEspacios() {
        assertThrows(
                IllegalArgumentException.class,
                () -> client.consultarUbicacion(EDIFICIO_ID, "   ")
        );
    }

    @Test
    void debeRechazarTipoNoSoportado() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> client.consultarUbicacion(
                        EDIFICIO_ID, "SALON"
                )
        );

        assertTrue(
                exception.getMessage().contains("SALON")
        );
    }

    @Test
    void debeConvertirErrorDelServidorEnServicioNoDisponible() {
        responder(500, """
                {
                    "mensaje": "Error interno"
                }
                """);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> client.consultarUbicacion(
                        EDIFICIO_ID, "EDIFICIO"
                )
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getStatusCode()
        );
    }

    @Test
    void debeConvertirErrorDeComunicacionEnServicioNoDisponible() {
        server.stop(0);
        server = null;

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> client.consultarUbicacion(
                        EDIFICIO_ID, "EDIFICIO"
                )
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getStatusCode()
        );
    }
}
