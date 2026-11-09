package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.dto.CambiarEstadoBrigadistaRequest;
import edu.eci.dosw.brigades_and_incidents.entity.Brigadista;
import edu.eci.dosw.brigades_and_incidents.entity.EstadoBrigadista;
import edu.eci.dosw.brigades_and_incidents.repository.BrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.EstadoBrigadistaRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivarBrigadistaServiceTest {

    @Mock
    private BrigadistaRepository brigadistaRepository;

    @Mock
    private EstadoBrigadistaRepository estadoBrigadistaRepository;

    @InjectMocks
    private ActivarBrigadistaService service;

    @Test
    void debeActivarUnBrigadistaRegistrado() {
        Brigadista brigadista = mock(Brigadista.class);
        EstadoBrigadista estadoActivo = mock(EstadoBrigadista.class);

        when(brigadistaRepository.findByUsuarioIdExterno("BRIG-001"))
                .thenReturn(Optional.of(brigadista));

        when(brigadista.getUsuarioIdExterno()).thenReturn("BRIG-001");

        when(estadoBrigadistaRepository.findByNombre("ACTIVO"))
                .thenReturn(Optional.of(estadoActivo));

        when(brigadistaRepository.save(brigadista))
                .thenReturn(brigadista);

        var response = service.cambiarEstado(
                "BRIG-001",
                new CambiarEstadoBrigadistaRequest("ACTIVO")
        );

        assertNotNull(response);
        assertNotNull(response.idActivacion());
        assertEquals("BRIG-001", response.brigadistaId());
        assertEquals("ACTIVO", response.estado());
        assertNotNull(response.fechaCambioEstado());

        verify(brigadista).setEstado(estadoActivo);
        verify(brigadistaRepository).save(brigadista);
    }

    @Test
    void debeDesactivarUnBrigadistaRegistrado() {
        Brigadista brigadista = mock(Brigadista.class);
        EstadoBrigadista estadoInactivo = mock(EstadoBrigadista.class);

        when(brigadistaRepository.findByUsuarioIdExterno("BRIG-002"))
                .thenReturn(Optional.of(brigadista));

        when(brigadista.getUsuarioIdExterno()).thenReturn("BRIG-002");

        when(estadoBrigadistaRepository.findByNombre("INACTIVO"))
                .thenReturn(Optional.of(estadoInactivo));

        when(brigadistaRepository.save(brigadista))
                .thenReturn(brigadista);

        var response = service.cambiarEstado(
                "BRIG-002",
                new CambiarEstadoBrigadistaRequest("INACTIVO")
        );

        assertNotNull(response);
        assertEquals("BRIG-002", response.brigadistaId());
        assertEquals("INACTIVO", response.estado());

        verify(brigadista).setEstado(estadoInactivo);
        verify(brigadistaRepository).save(brigadista);
    }

    @Test
    void noDebeAceptarUnEstadoDiferenteDeActivoOInactivo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.cambiarEstado(
                        "BRIG-001",
                        new CambiarEstadoBrigadistaRequest("SUSPENDIDO")
                )
        );

        verifyNoInteractions(brigadistaRepository);
        verifyNoInteractions(estadoBrigadistaRepository);
    }

    @Test
    void noDebeActivarUnBrigadistaQueNoEstaRegistrado() {
        when(brigadistaRepository.findByUsuarioIdExterno("BRIG-999"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.cambiarEstado(
                        "BRIG-999",
                        new CambiarEstadoBrigadistaRequest("ACTIVO")
                )
        );

        verify(brigadistaRepository, never()).save(any());
    }

    @Test
    void noDebeCambiarEstadoCuandoElIdentificadorEstaVacio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.cambiarEstado(
                        " ",
                        new CambiarEstadoBrigadistaRequest("ACTIVO")
                )
        );

        verifyNoInteractions(brigadistaRepository);
        verifyNoInteractions(estadoBrigadistaRepository);
    }
}