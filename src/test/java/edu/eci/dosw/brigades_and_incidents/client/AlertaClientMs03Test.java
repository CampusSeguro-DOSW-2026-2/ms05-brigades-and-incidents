package edu.eci.dosw.brigades_and_incidents.client;

import edu.eci.dosw.brigades_and_incidents.client.AlertaClient.NotificacionAsignacion;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertaClientMs03Test {

    private HttpServer server;

    @AfterEach
    void limpiar() {
        RequestContextHolder.resetRequestAttributes();
        if (server != null) {
            server.stop(0);
        }
    }

    /** MS03 falso que guarda las cabeceras recibidas y responde 201. */
    private AlertaClientMs03 clienteContra(AtomicReference<Headers> recibidas) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/v1/alertas", exchange -> {
            recibidas.set(exchange.getRequestHeaders());
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        server.start();
        return new AlertaClientMs03("http://localhost:" + server.getAddress().getPort(), "clave-ms05");
    }

    private void peticionDelCoordinador(String permisos) {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("X-User-Id", "actor-1");
        peticion.addHeader("X-User-Role", "COORDINADOR");
        peticion.addHeader("X-User-Permission", permisos);
        peticion.addHeader("X-Correlation-ID", "corr-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(peticion));
    }

    @Test
    void reenviaLaIdentidadCompletaDeQuienAsigna() throws IOException {
        AtomicReference<Headers> recibidas = new AtomicReference<>();
        AlertaClientMs03 cliente = clienteContra(recibidas);
        peticionDelCoordinador("ms03:alert:manage,ms05:incident:read");

        cliente.notificarAsignacion(aviso(Set.of("COORDINADOR"), "EDIFICIO"));

        Headers h = recibidas.get();
        assertEquals("clave-ms05", h.getFirst("X-Service-Key"));
        assertEquals("actor-1", h.getFirst("X-User-Id"));
        assertEquals("COORDINADOR", h.getFirst("X-User-Role"));
        assertEquals("ms03:alert:manage,ms05:incident:read", h.getFirst("X-User-Permission"));
        assertEquals("corr-1", h.getFirst("X-Correlation-ID"));
    }

    @Test
    void sinPermisosEnLaPeticionNoLlamaAMs03() throws IOException {
        AtomicReference<Headers> recibidas = new AtomicReference<>();
        AlertaClientMs03 cliente = clienteContra(recibidas);

        assertThrows(IllegalStateException.class,
                () -> cliente.notificarAsignacion(aviso(Set.of("COORDINADOR"), "EDIFICIO")));
        assertNull(recibidas.get());
    }

    private static final String EVENTO = "3f6c2a9e-8b1d-4c7a-9e2f-1a5b7c9d0e42";
    private static final String INCIDENTE = "11111111-2222-3333-4444-555555555555";
    private static final String BRIGADISTA = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    private NotificacionAsignacion aviso(Set<String> roles, String tipoUbicacion) {
        return new NotificacionAsignacion(BRIGADISTA, "actor-1", roles, EVENTO, INCIDENTE,
                "EMERGENCIA", "BLD-001", tipoUbicacion);
    }

    @Test
    void conPermisoDeAlertasRealesCreaAlertaManualOperativa() {
        var alerta = AlertaClientMs03.construirAlerta(aviso(Set.of("COORDINADOR"), "EDIFICIO"), "ms03:alert:manage,ms05:incident:read");

        assertEquals(List.of("OPERATIVA"), alerta.types());
        assertEquals("MANUAL", alerta.origin().type());
        assertNull(alerta.origin().simulationId());
        assertNull(alerta.purpose());
        assertEquals(List.of(new AlertaClientMs03.Destinatario("USER", BRIGADISTA)), alerta.recipients());
        assertEquals("BUILDING", alerta.scope().type());
        assertEquals(List.of("BLD-001"), alerta.scope().ids());
        assertTrue(alerta.activateImmediately());
        assertTrue(alerta.instructions().contains("prioridad emergencia"));
    }

    @Test
    void soloConPermisoDeSimulacrosCreaAlertaDelSimulacro() {
        var alerta = AlertaClientMs03.construirAlerta(aviso(Set.of("ORGANIZADOR"), "ZONA_SEGURA"), "ms03:alert:manage_drill");

        assertEquals(List.of("OTHER"), alerta.types());
        assertEquals("SIMULATION", alerta.origin().type());
        assertEquals(UUID.fromString(EVENTO), alerta.origin().simulationId());
        assertEquals("ZONE", alerta.scope().type());
        assertEquals(32, alerta.purpose().length());
        assertTrue(alerta.purpose().startsWith("A111111112222333"));
    }

    @Test
    void puntoDeEncuentroUsaTodoElCampus() {
        var alerta = AlertaClientMs03.construirAlerta(aviso(Set.of("COORDINADOR"), "PUNTO_ENCUENTRO"), "ms03:alert:manage");

        assertEquals("CAMPUS", alerta.scope().type());
        assertNull(alerta.scope().ids());
    }

    @Test
    void organizadorConEventoQueNoEsUuidNoPuedeAvisar() {
        var aviso = new NotificacionAsignacion(BRIGADISTA, "actor-1", Set.of("ORGANIZADOR"), "EVENTO-001",
                INCIDENTE, "EMERGENCIA", "BLD-001", "EDIFICIO");

        assertThrows(IllegalArgumentException.class, () -> AlertaClientMs03.construirAlerta(aviso, "ms03:alert:manage_drill"));
    }
}
