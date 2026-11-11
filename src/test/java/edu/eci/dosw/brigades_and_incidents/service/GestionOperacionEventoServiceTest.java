
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.dto.ActualizarEstadoEventoRequest;
import edu.eci.dosw.brigades_and_incidents.entity.ConfiguracionOperacionEvento;
import edu.eci.dosw.brigades_and_incidents.repository.ConfiguracionOperacionEventoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestionOperacionEventoServiceTest {

    @Mock
    private ConfiguracionOperacionEventoRepository repository;

    @InjectMocks
    private GestionOperacionEventoService service;

    private static final String EVENTO_ID = "evento-123";

    private ActualizarEstadoEventoRequest solicitud(String estado) {
        return new ActualizarEstadoEventoRequest(
                EVENTO_ID,
                "Simulacro de evacuación",
                "SISMO",
                false,
                estado,
                null,
                null,
                null
        );
    }

    @Test
    void debeCrearConfiguracionYHabilitarAutoasignacionCuandoEventoEsProgramado() {
        when(repository.findById(EVENTO_ID))
                .thenReturn(Optional.empty());

        service.actualizarOperacion(
                EVENTO_ID,
                solicitud("PROGRAMADO")
        );

        verify(repository).save(argThat(configuracion ->
                configuracion.getEventoId().equals(EVENTO_ID)
                        && configuracion.isAutoasignacionHabilitada()
                        && !configuracion.isPanelHabilitado()
        ));
    }

    @Test
    void debeHabilitarPanelCuandoEventoEstaEnEjecucion() {
        ConfiguracionOperacionEvento configuracion =
                new ConfiguracionOperacionEvento(EVENTO_ID);

        configuracion.habilitarAutoasignacion();

        when(repository.findById(EVENTO_ID))
                .thenReturn(Optional.of(configuracion));

        service.actualizarOperacion(
                EVENTO_ID,
                solicitud("EN_EJECUCION")
        );

        assertFalse(configuracion.isAutoasignacionHabilitada());
        assertTrue(configuracion.isPanelHabilitado());

        verify(repository).save(configuracion);
    }

    @Test
    void debeDeshabilitarOperacionCuandoEventoFinaliza() {
        ConfiguracionOperacionEvento configuracion =
                new ConfiguracionOperacionEvento(EVENTO_ID);

        configuracion.habilitarPanel();

        when(repository.findById(EVENTO_ID))
                .thenReturn(Optional.of(configuracion));

        service.actualizarOperacion(
                EVENTO_ID,
                solicitud("FINALIZADO")
        );

        assertFalse(configuracion.isAutoasignacionHabilitada());
        assertFalse(configuracion.isPanelHabilitado());

        verify(repository).save(configuracion);
    }

    @Test
    void debeDeshabilitarOperacionCuandoEventoEstaEnBorrador() {
        ConfiguracionOperacionEvento configuracion =
                new ConfiguracionOperacionEvento(EVENTO_ID);

        configuracion.habilitarAutoasignacion();

        when(repository.findById(EVENTO_ID))
                .thenReturn(Optional.of(configuracion));

        service.actualizarOperacion(
                EVENTO_ID,
                solicitud("BORRADOR")
        );

        assertFalse(configuracion.isAutoasignacionHabilitada());
        assertFalse(configuracion.isPanelHabilitado());

        verify(repository).save(configuracion);
    }

    @Test
    void debeAceptarEstadoConMinusculasYEspacios() {
        when(repository.findById(EVENTO_ID))
                .thenReturn(Optional.empty());

        service.actualizarOperacion(
                EVENTO_ID,
                solicitud("  programado  ")
        );

        verify(repository).save(argThat(
                ConfiguracionOperacionEvento::isAutoasignacionHabilitada
        ));
    }

    @Test
    void debeReutilizarConfiguracionExistente() {
        ConfiguracionOperacionEvento configuracion =
                new ConfiguracionOperacionEvento(EVENTO_ID);

        when(repository.findById(EVENTO_ID))
                .thenReturn(Optional.of(configuracion));

        service.actualizarOperacion(
                EVENTO_ID,
                solicitud("PROGRAMADO")
        );

        verify(repository).save(same(configuracion));
    }

    @Test
    void debeRechazarSolicitudNula() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.actualizarOperacion(EVENTO_ID, null)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void debeRechazarIdentificadorNuloEnSolicitud() {
        ActualizarEstadoEventoRequest request =
                new ActualizarEstadoEventoRequest(
                        null,
                        "Evento",
                        "SISMO",
                        false,
                        "PROGRAMADO",
                        null,
                        null,
                        null
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.actualizarOperacion(EVENTO_ID, request)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void debeRechazarIdentificadorDiferente() {
        ActualizarEstadoEventoRequest request =
                new ActualizarEstadoEventoRequest(
                        "otro-evento",
                        "Evento",
                        "SISMO",
                        false,
                        "PROGRAMADO",
                        null,
                        null,
                        null
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.actualizarOperacion(EVENTO_ID, request)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void debeRechazarEstadoNulo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.actualizarOperacion(
                        EVENTO_ID,
                        solicitud(null)
                )
        );

        verifyNoInteractions(repository);
    }

    @Test
    void debeRechazarEstadoVacio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.actualizarOperacion(
                        EVENTO_ID,
                        solicitud("")
                )
        );

        verifyNoInteractions(repository);
    }

    @Test
    void debeRechazarEstadoConSoloEspacios() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.actualizarOperacion(
                        EVENTO_ID,
                        solicitud("   ")
                )
        );

        verifyNoInteractions(repository);
    }

    @Test
    void debeRechazarEstadoNoSoportado() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.actualizarOperacion(
                        EVENTO_ID,
                        solicitud("CANCELADO")
                )
        );

        assertTrue(
                exception.getMessage().contains("CANCELADO")
        );

        verify(repository).findById(EVENTO_ID);
        verify(repository, never()).save(any());
    }

    @Test
    void debeObtenerConfiguracionExistente() {
        ConfiguracionOperacionEvento configuracion =
                new ConfiguracionOperacionEvento(EVENTO_ID);

        when(repository.findById(EVENTO_ID))
                .thenReturn(Optional.of(configuracion));

        ConfiguracionOperacionEvento resultado =
                service.obtenerConfiguracion(EVENTO_ID);

        assertSame(configuracion, resultado);
        verify(repository).findById(EVENTO_ID);
    }

    @Test
    void debeLanzarExcepcionCuandoNoExisteConfiguracion() {
        when(repository.findById(EVENTO_ID))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.obtenerConfiguracion(EVENTO_ID)
        );

        assertEquals(
                "No existe configuración operativa para el evento",
                exception.getMessage()
        );
    }
}
