package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.BusquedaBrigadistasResponse;
import edu.eci.dosw.brigades_and_incidents.dto.CambiarEstadoBrigadistaRequest;
import edu.eci.dosw.brigades_and_incidents.service.BuscarBrigadistaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BuscarBrigadistaControllerTest {

    private BuscarBrigadistaService service;
    private BuscarBrigadistaController controller;

    @BeforeEach
    void setUp() {
        service = mock(BuscarBrigadistaService.class);
        controller = new BuscarBrigadistaController(service);
    }

    @Test
    void debeBuscarBrigadistasComoCoordinador() {
        BusquedaBrigadistasResponse esperada =
                new BusquedaBrigadistasResponse(
                        "Brigadistas encontrados",
                        List.of()
                );

        when(service.buscarBrigadistas("Paula")).thenReturn(esperada);

        BusquedaBrigadistasResponse resultado =
                controller.buscarBrigadistas(
                        "coordinador-1",
                        "COORDINADOR",
                        "Paula"
                );

        assertSame(esperada, resultado);
        verify(service).buscarBrigadistas("Paula");
    }

    @Test
    void debeAceptarRolCoordinadorSinImportarMayusculasNiEspacios() {
        BusquedaBrigadistasResponse esperada =
                new BusquedaBrigadistasResponse("Sin resultados", List.of());

        when(service.buscarBrigadistas("Ana")).thenReturn(esperada);

        BusquedaBrigadistasResponse resultado =
                controller.buscarBrigadistas(
                        "coordinador-1",
                        " coordinador ",
                        "Ana"
                );

        assertSame(esperada, resultado);
        verify(service).buscarBrigadistas("Ana");
    }

    @Test
    void debeRechazarUsuarioNulo() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.buscarBrigadistas(
                        null, "COORDINADOR", "Ana"
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void debeRechazarUsuarioVacio() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.buscarBrigadistas(
                        " ", "COORDINADOR", "Ana"
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void debeRechazarRolNulo() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.buscarBrigadistas(
                        "usuario-1", null, "Ana"
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void debeRechazarRolVacio() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.buscarBrigadistas(
                        "usuario-1", " ", "Ana"
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void debeRechazarUsuarioQueNoEsCoordinador() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.buscarBrigadistas(
                        "brigadista-1", "BRIGADISTA", "Ana"
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void debeCambiarEstadoComoCoordinador() {
        CambiarEstadoBrigadistaRequest request =
                new CambiarEstadoBrigadistaRequest("ACTIVO");

        assertDoesNotThrow(() ->
                controller.cambiarEstado(
                        "brigadista-1",
                        "coordinador-1",
                        "COORDINADOR",
                        request
                )
        );

        verify(service).cambiarEstado("brigadista-1", "ACTIVO");
    }

    @Test
    void debeRechazarCambioEstadoSinUsuarioAutenticado() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.cambiarEstado(
                        "brigadista-1",
                        null,
                        "COORDINADOR",
                        new CambiarEstadoBrigadistaRequest("ACTIVO")
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void debeRechazarCambioEstadoConRolNoAutorizado() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.cambiarEstado(
                        "brigadista-1",
                        "usuario-1",
                        "BRIGADISTA",
                        new CambiarEstadoBrigadistaRequest("ACTIVO")
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(service);
    }
}