
package edu.eci.dosw.brigades_and_incidents.client;

import com.sun.net.httpserver.HttpServer;
import edu.eci.dosw.brigades_and_incidents.exception.Ms01IntegrationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BrigadistaClientMs01Test {

    private static final String SERVICE_KEY = "test-service-key";

    private HttpServer server;
    private BrigadistaClientMs01 client;
    private final AtomicInteger solicitudes = new AtomicInteger();

    private volatile String respuesta;
    private volatile int estadoHttp = 200;

    @BeforeEach
    void setUp() throws IOException {
        solicitudes.set(0);

        respuesta = """
                {
                    "contenido": [],
                    "page": 0,
                    "size": 100,
                    "total_elementos": 0,
                    "total_paginas": 1
                }
                """;

        estadoHttp = 200;

        server = HttpServer.create(
                new InetSocketAddress("localhost", 0),
                0
        );

        server.createContext("/api/v1/users/by-role", exchange -> {
            solicitudes.incrementAndGet();

            byte[] cuerpo = respuesta.getBytes(StandardCharsets.UTF_8);

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

        client = new BrigadistaClientMs01(
                baseUrl,
                SERVICE_KEY
        );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void debeListarBrigadistasYConvertirSusDatos() {
        respuesta = """
                {
                    "contenido": [
                        {
                            "id": "11111111-1111-1111-1111-111111111111",
                            "primer_nombre": "Paula",
                            "segundo_nombre": "Alejandra",
                            "primer_apellido": "Novoa",
                            "segundo_apellido": "Castellanos",
                            "roles": ["COORDINADOR", "BRIGADISTA"]
                        },
                        {
                            "id": "22222222-2222-2222-2222-222222222222",
                            "primer_nombre": "Samuel",
                            "segundo_nombre": null,
                            "primer_apellido": "Sierra",
                            "segundo_apellido": null,
                            "roles": ["brigadista"]
                        }
                    ],
                    "page": 0,
                    "size": 100,
                    "total_elementos": 2,
                    "total_paginas": 1
                }
                """;

        var resultado = client.listarBrigadistas();

        assertEquals(2, resultado.size());

        assertEquals(
                "11111111-1111-1111-1111-111111111111",
                resultado.get(0).id()
        );

        assertEquals(
                "Paula Alejandra Novoa Castellanos",
                resultado.get(0).nombre()
        );

        assertEquals("BRIGADISTA", resultado.get(0).rol());

        assertEquals(
                "Samuel Sierra",
                resultado.get(1).nombre()
        );

        assertEquals("brigadista", resultado.get(1).rol());

        assertEquals(1, solicitudes.get());
    }

    @Test
    void debeConsultarTodasLasPaginasDisponibles() {
        server.removeContext("/api/v1/users/by-role");

        server.createContext("/api/v1/users/by-role", exchange -> {
            solicitudes.incrementAndGet();

            String query = exchange.getRequestURI().getQuery();

            String cuerpo;

            if (query != null && query.contains("page=0")) {
                cuerpo = """
                        {
                            "contenido": [
                                {
                                    "id": "11111111-1111-1111-1111-111111111111",
                                    "primer_nombre": "Paula",
                                    "segundo_nombre": null,
                                    "primer_apellido": "Novoa",
                                    "segundo_apellido": null,
                                    "roles": ["BRIGADISTA"]
                                }
                            ],
                            "page": 0,
                            "size": 100,
                            "total_elementos": 2,
                            "total_paginas": 2
                        }
                        """;
            } else {
                cuerpo = """
                        {
                            "contenido": [
                                {
                                    "id": "22222222-2222-2222-2222-222222222222",
                                    "primer_nombre": "Samuel",
                                    "segundo_nombre": null,
                                    "primer_apellido": "Sierra",
                                    "segundo_apellido": null,
                                    "roles": ["BRIGADISTA"]
                                }
                            ],
                            "page": 1,
                            "size": 100,
                            "total_elementos": 2,
                            "total_paginas": 2
                        }
                        """;
            }

            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().add(
                    "Content-Type",
                    "application/json"
            );

            exchange.sendResponseHeaders(200, bytes.length);

            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }

            exchange.close();
        });

        var resultado = client.listarBrigadistas();

        assertEquals(2, resultado.size());
        assertEquals("Paula Novoa", resultado.get(0).nombre());
        assertEquals("Samuel Sierra", resultado.get(1).nombre());
        assertEquals(2, solicitudes.get());
    }

    @Test
    void debeRetornarListaVaciaCuandoContenidoEsNulo() {
        respuesta = """
                {
                    "contenido": null,
                    "page": 0,
                    "size": 100,
                    "total_elementos": 0,
                    "total_paginas": 1
                }
                """;

        var resultado = client.listarBrigadistas();

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void debeIgnorarUsuariosNulosDentroDelContenido() {
        respuesta = """
                {
                    "contenido": [
                        null,
                        {
                            "id": "11111111-1111-1111-1111-111111111111",
                            "primer_nombre": "Paula",
                            "segundo_nombre": null,
                            "primer_apellido": "Novoa",
                            "segundo_apellido": null,
                            "roles": ["BRIGADISTA"]
                        }
                    ],
                    "page": 0,
                    "size": 100,
                    "total_elementos": 2,
                    "total_paginas": 1
                }
                """;

        var resultado = client.listarBrigadistas();

        assertEquals(1, resultado.size());
        assertEquals("Paula Novoa", resultado.get(0).nombre());
    }

    @Test
    void debeLanzarExcepcionCuandoRespuestaEsVacia() {
        estadoHttp = 204;

        Ms01IntegrationException exception = assertThrows(
                Ms01IntegrationException.class,
                () -> client.listarBrigadistas()
        );

        assertTrue(
                exception.getMessage().contains(
                        "MS01 devolvió una respuesta vacía"
                )
        );
    }

    @Test
    void debeLanzarExcepcionCuandoNumeroDePaginasEsNegativo() {
        respuesta = """
                {
                    "contenido": [],
                    "page": 0,
                    "size": 100,
                    "total_elementos": 0,
                    "total_paginas": -1
                }
                """;

        Ms01IntegrationException exception = assertThrows(
                Ms01IntegrationException.class,
                () -> client.listarBrigadistas()
        );

        assertTrue(
                exception.getMessage().contains(
                        "número de páginas inválido"
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
                () -> client.listarBrigadistas()
        );

        assertTrue(
                exception.getMessage().contains(
                        "MS01 respondió con estado HTTP"
                )
        );
    }

    @Test
    void debeEnvolverErrorDeComunicacionConMs01() {
        server.stop(0);
        server = null;

        Ms01IntegrationException exception = assertThrows(
                Ms01IntegrationException.class,
                () -> client.listarBrigadistas()
        );

        assertTrue(
                exception.getMessage().contains(
                        "No fue posible comunicarse con MS01"
                )
        );

        assertNotNull(exception.getCause());
    }

    @Test
    void debeFallarCuandoUsuarioNoTieneIdentificador() {
        respuesta = """
                {
                    "contenido": [
                        {
                            "id": null,
                            "primer_nombre": "Paula",
                            "segundo_nombre": null,
                            "primer_apellido": "Novoa",
                            "segundo_apellido": null,
                            "roles": ["BRIGADISTA"]
                        }
                    ],
                    "page": 0,
                    "size": 100,
                    "total_elementos": 1,
                    "total_paginas": 1
                }
                """;

        assertThrows(
                NullPointerException.class,
                () -> client.listarBrigadistas()
        );
    }

    @Test
    void debePermitirNombresParcialesYRolesAusentes() {
        respuesta = """
                {
                    "contenido": [
                        {
                            "id": "11111111-1111-1111-1111-111111111111",
                            "primer_nombre": "  Paula  ",
                            "segundo_nombre": " ",
                            "primer_apellido": null,
                            "segundo_apellido": null,
                            "roles": ["COORDINADOR"]
                        }
                    ],
                    "page": 0,
                    "size": 100,
                    "total_elementos": 1,
                    "total_paginas": 1
                }
                """;

        var resultado = client.listarBrigadistas();

        assertEquals(1, resultado.size());
        assertEquals("Paula", resultado.get(0).nombre());
        assertNull(resultado.get(0).rol());
    }
}
