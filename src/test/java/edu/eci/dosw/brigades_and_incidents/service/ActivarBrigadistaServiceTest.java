package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.dto.ActivacionBrigadistaResponse;
import edu.eci.dosw.brigades_and_incidents.dto.CambiarEstadoBrigadistaRequest;
import edu.eci.dosw.brigades_and_incidents.entity.Brigadista;
import edu.eci.dosw.brigades_and_incidents.entity.EstadoBrigadista;
import edu.eci.dosw.brigades_and_incidents.repository.BrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.EstadoBrigadistaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivarBrigadistaServiceTest {

    @Mock
    private BrigadistaRepository brigadistaRepository;

    @Mock
    private EstadoBrigadistaRepository estadoBrigadistaRepository;

    @InjectMocks
    private ActivarBrigadistaService service;

    private Brigadista brigadista;
    private EstadoBrigadista estadoActivo;
    private EstadoBrigadista estadoInactivo;

    @BeforeEach
    void setUp() {
        estadoActivo = new EstadoBrigadista("ACTIVO", "Brigadista activo");
        estadoInactivo = new EstadoBrigadista("INACTIVO", "Brigadista inactivo");
        brigadista = new Brigadista("usuario-123", estadoInactivo);
    }

    @Test
    void debeActivarBrigadistaCorrectamente() {
        when(brigadistaRepository.findByUsuarioIdExterno("usuario-123"))
                .thenReturn(Optional.of(brigadista));
        when(estadoBrigadistaRepository.findByNombre("ACTIVO"))
                .thenReturn(Optional.of(estadoActivo));
        when(brigadistaRepository.save(brigadista)).thenReturn(brigadista);

        ActivacionBrigadistaResponse response = service.cambiarEstado(
                "usuario-123",
                new CambiarEstadoBrigadistaRequest("ACTIVO")
        );

        assertNotNull(response);
        assertNotNull(response.idActivacion());
        assertEquals("usuario-123", response.brigadistaId());
        assertEquals("ACTIVO", response.estado());
        assertNotNull(response.fechaCambioEstado());
        assertSame(estadoActivo, brigadista.getEstado());

        verify(brigadistaRepository).save(brigadista);
    }

    @Test
    void debeInactivarBrigadistaCorrectamente() {
        when(brigadistaRepository.findByUsuarioIdExterno("usuario-123"))
                .thenReturn(Optional.of(brigadista));
        when(estadoBrigadistaRepository.findByNombre("INACTIVO"))
                .thenReturn(Optional.of(estadoInactivo));

        ActivacionBrigadistaResponse response = service.cambiarEstado(
                "usuario-123",
                new CambiarEstadoBrigadistaRequest("INACTIVO")
        );

        assertEquals("INACTIVO", response.estado());
        assertSame(estadoInactivo, brigadista.getEstado());
        verify(brigadistaRepository).save(brigadista);
    }

    @Test
    void debeAceptarEstadoConEspaciosYMinusculas() {
        when(brigadistaRepository.findByUsuarioIdExterno("usuario-123"))
                .thenReturn(Optional.of(brigadista));
        when(estadoBrigadistaRepository.findByNombre("ACTIVO"))
                .thenReturn(Optional.of(estadoActivo));

        ActivacionBrigadistaResponse response = service.cambiarEstado(
                "usuario-123",
                new CambiarEstadoBrigadistaRequest(" activo ")
        );

        assertEquals("ACTIVO", response.estado());
        verify(brigadistaRepository).save(brigadista);
    }

    @Test
    void debeRechazarIdentificadorNulo() {
        assertThrows(IllegalArgumentException.class, () ->
                service.cambiarEstado(
                        null,
                        new CambiarEstadoBrigadistaRequest("ACTIVO")
                ));

        verifyNoInteractions(brigadistaRepository, estadoBrigadistaRepository);
    }

    @Test
    void debeRechazarIdentificadorVacio() {
        assertThrows(IllegalArgumentException.class, () ->
                service.cambiarEstado(
                        " ",
                        new CambiarEstadoBrigadistaRequest("ACTIVO")
                ));

        verifyNoInteractions(brigadistaRepository, estadoBrigadistaRepository);
    }

    @Test
    void debeRechazarSolicitudNula() {
        assertThrows(IllegalArgumentException.class, () ->
                service.cambiarEstado("usuario-123", null));

        verifyNoInteractions(brigadistaRepository, estadoBrigadistaRepository);
    }

    @Test
    void debeRechazarEstadoNulo() {
        assertThrows(IllegalArgumentException.class, () ->
                service.cambiarEstado(
                        "usuario-123",
                        new CambiarEstadoBrigadistaRequest(null)
                ));

        verifyNoInteractions(brigadistaRepository, estadoBrigadistaRepository);
    }

    @Test
    void debeRechazarEstadoVacio() {
        assertThrows(IllegalArgumentException.class, () ->
                service.cambiarEstado(
                        "usuario-123",
                        new CambiarEstadoBrigadistaRequest(" ")
                ));

        verifyNoInteractions(brigadistaRepository, estadoBrigadistaRepository);
    }

    @Test
    void debeRechazarEstadoNoPermitido() {
        assertThrows(IllegalArgumentException.class, () ->
                service.cambiarEstado(
                        "usuario-123",
                        new CambiarEstadoBrigadistaRequest("SUSPENDIDO")
                ));

        verifyNoInteractions(brigadistaRepository, estadoBrigadistaRepository);
    }

    @Test
    void debeRechazarBrigadistaInexistente() {
        when(brigadistaRepository.findByUsuarioIdExterno("desconocido"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                service.cambiarEstado(
                        "desconocido",
                        new CambiarEstadoBrigadistaRequest("ACTIVO")
                ));

        verify(brigadistaRepository, never()).save(any());
    }

    @Test
    void debeRechazarEstadoQueNoExisteEnBaseDeDatos() {
        when(brigadistaRepository.findByUsuarioIdExterno("usuario-123"))
                .thenReturn(Optional.of(brigadista));
        when(estadoBrigadistaRepository.findByNombre("ACTIVO"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () ->
                service.cambiarEstado(
                        "usuario-123",
                        new CambiarEstadoBrigadistaRequest("ACTIVO")
                ));

        verify(brigadistaRepository, never()).save(any());
    }
}