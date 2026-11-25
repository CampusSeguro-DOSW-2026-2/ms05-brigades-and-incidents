package edu.eci.dosw.brigades_and_incidents.client;

import edu.eci.dosw.brigades_and_incidents.client.AlertaClient.NotificacionAsignacion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AlertaClientMs03Test {

    private static final String EVENTO = "3f6c2a9e-8b1d-4c7a-9e2f-1a5b7c9d0e42";
    private static final String INCIDENTE = "11111111-2222-3333-4444-555555555555";
    private static final String BRIGADISTA = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String ACTOR = "bbbbbbbb-cccc-dddd-eeee-ffffffffffff";
    private static final String ROLES = "COORDINADOR,ADMIN";
    private static final String PERMISOS = "ms05:assistance:assign_to_real,ms03:alert:manage";

    private NotificacionAsignacion aviso(Set<String> roles, String tipoUbicacion) {
        return new NotificacionAsignacion(BRIGADISTA, "actor-1", roles, EVENTO, INCIDENTE,
                "EMERGENCIA", "BLD-001", tipoUbicacion);
    }

    @AfterEach
    void limpiarContextoDeSolicitud() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void coordinadorCreaAlertaManualOperativaDirigidaAlBrigadista() {
        var alerta = AlertaClientMs03.construirAlerta(aviso(Set.of("COORDINADOR"), "EDIFICIO"));

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
    void organizadorCreaAlertaDelSimulacroConPropositoPropio() {
        var alerta = AlertaClientMs03.construirAlerta(aviso(Set.of("ORGANIZADOR"), "ZONA_SEGURA"));

        assertEquals(List.of("OTHER"), alerta.types());
        assertEquals("SIMULATION", alerta.origin().type());
        assertEquals(UUID.fromString(EVENTO), alerta.origin().simulationId());
        assertEquals("ZONE", alerta.scope().type());
        assertEquals(32, alerta.purpose().length());
        assertTrue(alerta.purpose().startsWith("A111111112222333"));
    }

    @Test
    void puntoDeEncuentroUsaTodoElCampus() {
        var alerta = AlertaClientMs03.construirAlerta(aviso(Set.of("COORDINADOR"), "PUNTO_ENCUENTRO"));

        assertEquals("CAMPUS", alerta.scope().type());
        assertNull(alerta.scope().ids());
    }

    @Test
    void organizadorConEventoQueNoEsUuidNoPuedeAvisar() {
        var aviso = new NotificacionAsignacion(BRIGADISTA, "actor-1", Set.of("ORGANIZADOR"), "EVENTO-001",
                INCIDENTE, "EMERGENCIA", "BLD-001", "EDIFICIO");

        assertThrows(IllegalArgumentException.class, () -> AlertaClientMs03.construirAlerta(aviso));
    }

    /**
     * El Gateway y MS03 rechazan una identidad parcial: las tres cabeceras del usuario que asigna
     * ({@code X-User-Id}, {@code X-User-Role} y {@code X-User-Permission}) viajan juntas, tal como llegaron
     * validadas en la solicitud en curso.
     */
    @Test
    void propagaLaIdentidadCompletaDelUsuarioQueAsigna() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        AlertaClientMs03 cliente = new AlertaClientMs03(builder, "http://gateway", "clave-de-servicio");

        MockHttpServletRequest solicitud = new MockHttpServletRequest();
        solicitud.addHeader("X-User-Id", ACTOR);
        solicitud.addHeader("X-User-Role", ROLES);
        solicitud.addHeader("X-User-Permission", PERMISOS);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(solicitud));

        servidor.expect(requestTo("http://gateway/api/v1/alertas"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-User-Id", ACTOR))
                .andExpect(header("X-User-Role", ROLES))
                .andExpect(header("X-User-Permission", PERMISOS))
                .andRespond(withSuccess());

        cliente.notificarAsignacion(aviso(Set.of("COORDINADOR"), "EDIFICIO"));

        servidor.verify();
    }
}
