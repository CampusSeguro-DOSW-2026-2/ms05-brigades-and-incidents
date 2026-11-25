package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.entity.EstadoIncidente;
import edu.eci.dosw.brigades_and_incidents.entity.Incidente;
import edu.eci.dosw.brigades_and_incidents.repository.IncidenteRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IndicadoresEventoServiceTest {

    private static final String EVENTO = "EVENTO-001";
    private static final OffsetDateTime T0 = OffsetDateTime.parse("2026-11-25T10:00:00-05:00");

    private final IncidenteRepository repositorio = mock(IncidenteRepository.class);
    private final IndicadoresEventoService service = new IndicadoresEventoService(repositorio);

    private static Incidente incidente(String estado, String responsable, Integer segundosHastaAtencion, boolean eliminado) {
        EstadoIncidente e = new EstadoIncidente(estado, estado);
        Incidente i = new Incidente();
        i.setId(UUID.randomUUID());
        i.setEventoId(EVENTO);
        i.setEstado(e);
        i.setFechaReporte(T0);
        i.setBrigadistaResponsableId(responsable);
        if (segundosHastaAtencion != null) {
            i.setFechaPrimeraAtencion(T0.plusSeconds(segundosHastaAtencion));
        }
        i.setEliminado(eliminado);
        return i;
    }

    @Test
    void calculaLosIndicadoresConLosIncidentesDelEvento() {
        when(repositorio.findByEventoId(EVENTO)).thenReturn(List.of(
                incidente("REPORTADO", null, null, false),
                incidente("ASIGNADO", "BRIG-1", null, false),
                incidente("EN_ATENCION", "BRIG-1", 60, false),
                incidente("RESUELTO", "BRIG-2", 120, false),
                incidente("RESUELTO", "BRIG-2", 300, true)));

        var r = service.consultar(EVENTO);

        assertEquals(EVENTO, r.eventoId());
        assertEquals(4, r.totalIncidentes());
        assertEquals(2, r.totalCasosAtendidos());
        assertEquals(1, r.totalCasosResueltos());
        assertEquals(90L, r.tiempoPromedioAtencionSegundos());
        assertEquals(4, r.incidentes().size());
        assertEquals(2, r.porBrigadista().size());
        var brig1 = r.porBrigadista().stream().filter(b -> b.brigadistaId().equals("BRIG-1")).findFirst().orElseThrow();
        assertEquals(1, brig1.casosAtendidos());
        assertEquals(0, brig1.casosResueltos());
        var brig2 = r.porBrigadista().stream().filter(b -> b.brigadistaId().equals("BRIG-2")).findFirst().orElseThrow();
        assertEquals(1, brig2.casosAtendidos());
        assertEquals(1, brig2.casosResueltos());
    }

    @Test
    void sinIncidentesTodoEnCeroYSinPromedio() {
        when(repositorio.findByEventoId(EVENTO)).thenReturn(List.of());

        var r = service.consultar(EVENTO);

        assertEquals(0, r.totalIncidentes());
        assertEquals(0, r.totalCasosAtendidos());
        assertNull(r.tiempoPromedioAtencionSegundos());
        assertTrue(r.porBrigadista().isEmpty());
        assertTrue(r.incidentes().isEmpty());
    }
}
