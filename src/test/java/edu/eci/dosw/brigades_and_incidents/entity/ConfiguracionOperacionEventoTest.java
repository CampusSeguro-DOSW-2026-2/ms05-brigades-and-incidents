
package edu.eci.dosw.brigades_and_incidents.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ConfiguracionOperacionEventoTest {

    private ConfiguracionOperacionEvento configuracion;

    @BeforeEach
    void setUp() {
        configuracion = new ConfiguracionOperacionEvento("evento-123");
    }

    @Test
    void debeCrearConfiguracionConOpcionesDeshabilitadas() {
        assertEquals("evento-123", configuracion.getEventoId());
        assertFalse(configuracion.isAutoasignacionHabilitada());
        assertFalse(configuracion.isPanelHabilitado());
        assertNotNull(configuracion.getActualizadoEn());
    }

    @Test
    void debeHabilitarAutoasignacionYDeshabilitarPanel() {
        configuracion.habilitarPanel();

        configuracion.habilitarAutoasignacion();

        assertTrue(configuracion.isAutoasignacionHabilitada());
        assertFalse(configuracion.isPanelHabilitado());
    }

    @Test
    void debeHabilitarPanelYDeshabilitarAutoasignacion() {
        configuracion.habilitarAutoasignacion();

        configuracion.habilitarPanel();

        assertFalse(configuracion.isAutoasignacionHabilitada());
        assertTrue(configuracion.isPanelHabilitado());
    }

    @Test
    void debeDeshabilitarAmbasOpciones() {
        configuracion.habilitarAutoasignacion();

        configuracion.deshabilitarOperacion();

        assertFalse(configuracion.isAutoasignacionHabilitada());
        assertFalse(configuracion.isPanelHabilitado());
    }

    @Test
    void debeActualizarFechaAlHabilitarAutoasignacion() {
        OffsetDateTime fechaAnterior = configuracion.getActualizadoEn();

        configuracion.habilitarAutoasignacion();

        assertNotNull(configuracion.getActualizadoEn());
        assertFalse(
                configuracion.getActualizadoEn().isBefore(fechaAnterior)
        );
    }

    @Test
    void debeActualizarFechaAlHabilitarPanel() {
        OffsetDateTime fechaAnterior = configuracion.getActualizadoEn();

        configuracion.habilitarPanel();

        assertNotNull(configuracion.getActualizadoEn());
        assertFalse(
                configuracion.getActualizadoEn().isBefore(fechaAnterior)
        );
    }

    @Test
    void debeActualizarFechaAlDeshabilitarOperacion() {
        OffsetDateTime fechaAnterior = configuracion.getActualizadoEn();

        configuracion.deshabilitarOperacion();

        assertNotNull(configuracion.getActualizadoEn());
        assertFalse(
                configuracion.getActualizadoEn().isBefore(fechaAnterior)
        );
    }

    @Test
    void debePermitirHabilitarAutoasignacionVariasVeces() {
        configuracion.habilitarAutoasignacion();
        configuracion.habilitarAutoasignacion();

        assertTrue(configuracion.isAutoasignacionHabilitada());
        assertFalse(configuracion.isPanelHabilitado());
    }

    @Test
    void debePermitirHabilitarPanelVariasVeces() {
        configuracion.habilitarPanel();
        configuracion.habilitarPanel();

        assertFalse(configuracion.isAutoasignacionHabilitada());
        assertTrue(configuracion.isPanelHabilitado());
    }

    @Test
    void debePermitirDeshabilitarOperacionVariasVeces() {
        configuracion.deshabilitarOperacion();
        configuracion.deshabilitarOperacion();

        assertFalse(configuracion.isAutoasignacionHabilitada());
        assertFalse(configuracion.isPanelHabilitado());
    }
}
