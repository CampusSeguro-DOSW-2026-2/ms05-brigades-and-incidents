package edu.eci.dosw.brigades_and_incidents.client;

import edu.eci.dosw.brigades_and_incidents.client.AlertaClient.NotificacionAsignacion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertaClientMs03Test {

    private static final String EVENTO = "3f6c2a9e-8b1d-4c7a-9e2f-1a5b7c9d0e42";
    private static final String INCIDENTE = "11111111-2222-3333-4444-555555555555";
    private static final String BRIGADISTA = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    private NotificacionAsignacion aviso(Set<String> roles, String tipoUbicacion) {
        return new NotificacionAsignacion(BRIGADISTA, "actor-1", roles, EVENTO, INCIDENTE,
                "EMERGENCIA", "BLD-001", tipoUbicacion);
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
}
