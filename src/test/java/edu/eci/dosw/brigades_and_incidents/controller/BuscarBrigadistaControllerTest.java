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
    void debeBuscarBrigadistas() {
        BusquedaBrigadistasResponse esperada =
                new BusquedaBrigadistasResponse(
                        "Brigadistas encontrados",
                        List.of()
                );

        when(service.buscarBrigadistas("Paula")).thenReturn(esperada);

        BusquedaBrigadistasResponse resultado =
                controller.buscarBrigadistas("coordinador-1", "Paula");

        assertSame(esperada, resultado);
        verify(service).buscarBrigadistas("Paula");
    }

    @Test
    void debeRechazarUsuarioNulo() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.buscarBrigadistas(null, "Ana")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void debeRechazarUsuarioVacio() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.buscarBrigadistas(" ", "Ana")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void debeCambiarEstado() {
        CambiarEstadoBrigadistaRequest request =
                new CambiarEstadoBrigadistaRequest("ACTIVO");

        assertDoesNotThrow(() ->
                controller.cambiarEstado("brigadista-1", "coordinador-1", request)
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
                        new CambiarEstadoBrigadistaRequest("ACTIVO")
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(service);
    }
}
