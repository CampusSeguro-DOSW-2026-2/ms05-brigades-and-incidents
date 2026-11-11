package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.entity.AsignacionBrigadista;
import edu.eci.dosw.brigades_and_incidents.repository.AsignacionBrigadistaRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultaBrigadistasEventoServiceTest {

    @Mock
    private AsignacionBrigadistaRepository asignacionRepository;

    @InjectMocks
    private ConsultaBrigadistasEventoService service;

    @Test
    void debeConsultarBrigadistasVigentesDeUnEvento() {
        AsignacionBrigadista asignacion = mock(AsignacionBrigadista.class);

        when(asignacion.getBrigadistaId()).thenReturn("BRIG-001");
        when(asignacion.getUbicacionId()).thenReturn("ZONA-001");

        when(asignacionRepository.findByEventoIdAndEstado_Nombre(
                "EVENTO-001", "VIGENTE"
        )).thenReturn(List.of(asignacion));

        var response = service.consultar("EVENTO-001");

        assertNotNull(response);
        assertEquals("EVENTO-001", response.eventoId());
        assertNotNull(response.actualizadoEn());
        assertEquals(1, response.brigadistas().size());

        var brigadista = response.brigadistas().get(0);

        assertEquals("BRIG-001", brigadista.brigadistaId());
        assertEquals("ZONA-001", brigadista.zonaId());
        assertEquals("ACTIVO", brigadista.estado());

        verify(asignacionRepository).findByEventoIdAndEstado_Nombre(
                "EVENTO-001", "VIGENTE"
        );
    }

    @Test
    void debeRetornarListaVaciaCuandoElEventoNoTieneBrigadistasVigentes() {
        when(asignacionRepository.findByEventoIdAndEstado_Nombre(
                "EVENTO-002", "VIGENTE"
        )).thenReturn(List.of());

        var response = service.consultar("EVENTO-002");

        assertNotNull(response);
        assertEquals("EVENTO-002", response.eventoId());
        assertTrue(response.brigadistas().isEmpty());
    }
}