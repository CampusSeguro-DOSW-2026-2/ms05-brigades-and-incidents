package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.ActivacionBrigadistaResponse;
import edu.eci.dosw.brigades_and_incidents.dto.CambiarEstadoBrigadistaRequest;
import edu.eci.dosw.brigades_and_incidents.service.ActivarBrigadistaService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActivacionBrigadistaControllerTest {

    private final ActivarBrigadistaService service =
            mock(ActivarBrigadistaService.class);

    private final ActivacionBrigadistaController controller =
            new ActivacionBrigadistaController(service);

    @Test
    void debeCambiarEstadoYRetornarRespuestaExitosa() {
        CambiarEstadoBrigadistaRequest request =
                new CambiarEstadoBrigadistaRequest("ACTIVO");

        ActivacionBrigadistaResponse responseEsperada =
                new ActivacionBrigadistaResponse(
                        UUID.randomUUID(),
                        "usuario-123",
                        "ACTIVO",
                        OffsetDateTime.now()
                );

        when(service.cambiarEstado("usuario-123", request))
                .thenReturn(responseEsperada);

        var response = controller.cambiarEstado("usuario-123", request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(responseEsperada, response.getBody());

        verify(service).cambiarEstado("usuario-123", request);
    }

    @Test
    void debePropagarErrorCuandoElServicioRechazaLaSolicitud() {
        CambiarEstadoBrigadistaRequest request =
                new CambiarEstadoBrigadistaRequest("SUSPENDIDO");

        when(service.cambiarEstado("usuario-123", request))
                .thenThrow(new IllegalArgumentException("Estado no permitido"));

        assertThrows(IllegalArgumentException.class, () ->
                controller.cambiarEstado("usuario-123", request));

        verify(service).cambiarEstado("usuario-123", request);
    }
}