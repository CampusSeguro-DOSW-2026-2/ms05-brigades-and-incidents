package edu.eci.dosw.brigades_and_incidents.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IndicadoresEventoServiceTest {

    private final IndicadoresEventoService service =
            new IndicadoresEventoService();

    @Test
    void debeRetornarRespuestaDeIndicadoresParaElEvento() {
        var response = service.consultar("EVENTO-001");

        assertNotNull(response);
        assertEquals("EVENTO-001", response.eventoId());
        assertNotNull(response.actualizadoEn());

        // Comportamiento temporal actual: los indicadores aún no se calculan.
        assertEquals(0, response.totalIncidentes());
        assertEquals(0, response.totalCasosAtendidos());
        assertEquals(0, response.totalCasosResueltos());

        assertTrue(response.porBrigadista().isEmpty());
        assertTrue(response.incidentes().isEmpty());
    }
}