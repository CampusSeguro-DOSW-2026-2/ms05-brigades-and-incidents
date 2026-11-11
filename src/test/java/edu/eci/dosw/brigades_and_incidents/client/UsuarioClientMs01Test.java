
package edu.eci.dosw.brigades_and_incidents.client;

import com.sun.net.httpserver.HttpServer;
import edu.eci.dosw.brigades_and_incidents.exception.Ms01IntegrationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioClientMs01Test {

    private static final String USUARIO_ID =
            "11111111-1111-1111-1111-111111111111";

    private HttpServer server;
    private UsuarioClientMs01 client;

    private String respuesta;
    private int estadoHttp = 200;

    @BeforeEach
    void setUp() throws IOException {
        respuesta = """
                {
                    "usuarios": [],
                    "no_encontrados": []
                }
                """;

        estadoHttp = 200;

        server = HttpServer.create(
                new InetSocketAddress("localhost", 0),
                0
        );

        server.createContext("/api/v1/users/batch", exchange -> {
            byte[] cuerpo = respuesta.getBytes(StandardCharsets.UTF_8);

            exchange.getRequestBody().readAllBytes();

            exchange.getResponseHeaders().add(
                    "Content-Type",
                    "application/json"
            );

            if (estadoHttp == 204) {
                exchange.sendResponseHeaders(204, -1);
            } else {
                exchange.sendResponseHeaders(
                        estadoHttp,
                        cuerpo.length
                );

                try (var output = exchange.getResponseBody()) {
                    output.write(cuerpo);
                }
            }

            exchange.close();
        });

        server.start();

        String baseUrl = "http://localhost:"
                + server.getAddress().getPort();

        client = new UsuarioClientMs01(
                baseUrl,
                "test-service-key"
        );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void debeConsultarUsuarioYConvertirDatosCorrectamente() {
        respuesta = """
                {
                    "usuarios": [
                        {
                            "id": "11111111-1111-1111-1111-111111111111",
                            "primer_nombre": "Paula",
                            "segundo_nombre": "Alejandra",
                            "primer_apellido": "Novoa",
                            "segundo_apellido": "Castellanos",
                            "roles": ["BRIGADISTA"],
                            "estado": "ACTIVO"
                        }
                    ],
                    "no_encontrados": []
                }
                """;

        UsuarioClient.UsuarioExterno usuario =
                client.consultarUsuario(USUARIO_ID);

        assertNotNull(usuario);
        assertEquals(USUARIO_ID, usuario.id());
        assertEquals(
                "Paula Alejandra Novoa Castellanos",
                usuario.nombreCompleto()
        );
        assertEquals(
                java.util.List.of("BRIGADISTA"),
                usuario.roles()
        );
        assertEquals("ACTIVO", usuario.estado());
    }

    @Test
    void debeEnviarSolicitudPostConIdentificador() {
        respuesta = """
                {
                    "usuarios": [
                        {
                            "id": "11111111-1111-1111-1111-111111111111",
                            "primer_nombre": "Paula",
                            "segundo_nombre": null,
                            "primer_apellido": "Novoa",
                            "segundo_apellido": null,
                            "roles": ["BRIGADISTA"],
                            "estado": "ACTIVO"
                        }
                    ],
                    "no_encontrados": []
                }
                """;

        assertNotNull(client.consultarUsuario(USUARIO_ID));
    }

    @Test
    void debeRechazarIdentificadorQueNoSeaUuid() {
        Ms01IntegrationException exception = assertThrows(
                Ms01IntegrationException.class,
                () -> client.consultarUsuario("no-es-un-uuid")
        );

        assertTrue(
                exception.getMessage().contains(
                        "El identificador del usuario no es un UUID válido"
                )
        );
    }

    @Test
    void debeRechazarIdentificadorNulo() {
        Ms01IntegrationException exception = assertThrows(
                Ms01IntegrationException.class,
                () -> client.consultarUsuario(null)
        );

        assertTrue(
                exception.getMessage().contains(
                        "El identificador del usuario no es un UUID válido"
                )
        );
    }

    @Test
    void debeLanzarExcepcionCuandoMs01RetornaErrorHttp() {
        estadoHttp = 500;
        respuesta = """
                {
                    "mensaje": "Error interno"
                }
                """;

        Ms01IntegrationException exception = assertThrows(
                Ms01IntegrationException.class,
                () -> client.consultarUsuario(USUARIO_ID)
        );

        assertTrue(
                exception.getMessage().contains(
                        "MS01 respondió con estado HTTP"
                )
        );
    }

    @Test
    void debeLanzarExcepcionCuandoRespuestaEsVacia() {
        estadoHttp = 204;

        Ms01IntegrationException exception = assertThrows(
                Ms01IntegrationException.class,
                () -> client.consultarUsuario(USUARIO_ID)
        );

        assertTrue(
                exception.getMessage().contains(
                        "respuesta vacía o inválida"
                )
        );
    }

    @Test
    void debeLanzarExcepcionCuandoListaUsuariosEsNula() {
        respuesta = """
                {
                    "usuarios": null,
                    "no_encontrados": []
                }
                """;

        Ms01IntegrationException exception = assertThrows(
                Ms01IntegrationException.class,
                () -> client.consultarUsuario(USUARIO_ID)
        );

        assertTrue(
                exception.getMessage().contains(
                        "respuesta vacía o inválida"
                )
        );
    }

    @Test
    void debeRetornarNuloCuandoUsuarioNoEstaEnLaRespuesta() {
        respuesta = """
                {
                    "usuarios": [
                        {
                            "id": "22222222-2222-2222-2222-222222222222",
                            "primer_nombre": "Samuel",
                            "segundo_nombre": null,
                            "primer_apellido": "Sierra",
                            "segundo_apellido": null,
                            "roles": ["BRIGADISTA"],
                            "estado": "ACTIVO"
                        }
                    ],
                    "no_encontrados": []
                }
                """;

        UsuarioClient.UsuarioExterno usuario =
                client.consultarUsuario(USUARIO_ID);

        assertNull(usuario);
    }

    @Test
    void debeIgnorarUsuariosNulosEnLaRespuesta() {
        respuesta = """
                {
                    "usuarios": [
                        null,
                        {
                            "id": "11111111-1111-1111-1111-111111111111",
                            "primer_nombre": "Paula",
                            "segundo_nombre": null,
                            "primer_apellido": "Novoa",
                            "segundo_apellido": null,
                            "roles": ["BRIGADISTA"],
                            "estado": "ACTIVO"
                        }
                    ],
                    "no_encontrados": []
                }
                """;

        UsuarioClient.UsuarioExterno usuario =
                client.consultarUsuario(USUARIO_ID);

        assertNotNull(usuario);
        assertEquals("Paula Novoa", usuario.nombreCompleto());
    }

    @Test
    void debePermitirRolesNulosYConvertirlosEnListaVacia() {
        respuesta = """
                {
                    "usuarios": [
                        {
                            "id": "11111111-1111-1111-1111-111111111111",
                            "primer_nombre": "Paula",
                            "segundo_nombre": null,
                            "primer_apellido": "Novoa",
                            "segundo_apellido": null,
                            "roles": null,
                            "estado": "ACTIVO"
                        }
                    ],
                    "no_encontrados": []
                }
                """;

        UsuarioClient.UsuarioExterno usuario =
                client.consultarUsuario(USUARIO_ID);

        assertNotNull(usuario);
        assertTrue(usuario.roles().isEmpty());
    }

    @Test
    void debeConvertirNombreConCamposNulosOVacios() {
        respuesta = """
                {
                    "usuarios": [
                        {
                            "id": "11111111-1111-1111-1111-111111111111",
                            "primer_nombre": "  Paula  ",
                            "segundo_nombre": " ",
                            "primer_apellido": null,
                            "segundo_apellido": "",
                            "roles": [],
                            "estado": "ACTIVO"
                        }
                    ],
                    "no_encontrados": []
                }
                """;

        UsuarioClient.UsuarioExterno usuario =
                client.consultarUsuario(USUARIO_ID);

        assertNotNull(usuario);
        assertEquals("Paula", usuario.nombreCompleto());
        assertTrue(usuario.roles().isEmpty());
    }

    @Test
    void debeEnvolverErrorDeComunicacionConMs01() {
        server.stop(0);
        server = null;

        Ms01IntegrationException exception = assertThrows(
                Ms01IntegrationException.class,
                () -> client.consultarUsuario(USUARIO_ID)
        );

        assertTrue(
                exception.getMessage().contains(
                        "No fue posible consultar el usuario en MS01"
                )
        );

        assertNotNull(exception.getCause());
    }
}
