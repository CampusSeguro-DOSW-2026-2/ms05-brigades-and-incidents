package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.ActualizarEstadoEventoRequest;
import edu.eci.dosw.brigades_and_incidents.dto.BrigadistaAsignadoDTO;
import edu.eci.dosw.brigades_and_incidents.dto.BrigadistaIndicadorDTO;
import edu.eci.dosw.brigades_and_incidents.dto.BrigadistasEventoResponse;
import edu.eci.dosw.brigades_and_incidents.dto.IndicadoresEventoResponse;
import edu.eci.dosw.brigades_and_incidents.security.SecurityConfig;
import edu.eci.dosw.brigades_and_incidents.service.ConsultaBrigadistasEventoService;
import edu.eci.dosw.brigades_and_incidents.service.GestionOperacionEventoService;
import edu.eci.dosw.brigades_and_incidents.service.IndicadoresEventoService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(IntegracionMs06Controller.class)
class IntegracionMs06ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GestionOperacionEventoService operacionService;

    @MockitoBean
    private ConsultaBrigadistasEventoService brigadistasService;

    @MockitoBean
    private IndicadoresEventoService indicadoresService;

    private MockHttpServletRequestBuilder comoServicioMs06(
            MockHttpServletRequestBuilder solicitud
    ) {
        return solicitud
                // El Gateway deriva X-Service-Name de la clave de MS06 (Estándares §5)
                .header("X-Service-Name", "ms06");
    }

    @Test
    void actualizarEstadoDebeRetornar204YDelegarEnElServicio()
            throws Exception {
        mockMvc.perform(
                comoServicioMs06(put("/api/v1/bei/eventos/EVT-1/estado"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventoId\":\"EVT-1\",\"estado\":\"ACTIVO\",\"simulacro\":false}")
        )
                .andExpect(status().isNoContent());

        verify(operacionService).actualizarOperacion(
                eq("EVT-1"),
                any(ActualizarEstadoEventoRequest.class)
        );
    }

    @Test
    void consultarBrigadistasDebeRetornarLosDelEvento() throws Exception {
        when(brigadistasService.consultar("EVT-1")).thenReturn(
                new BrigadistasEventoResponse(
                        "EVT-1",
                        OffsetDateTime.parse("2026-11-09T20:00:00-05:00"),
                        List.of(new BrigadistaAsignadoDTO(
                                "BRIG-001", "Ana", "ZONA-1", "ACTIVO"))
                )
        );

        mockMvc.perform(
                comoServicioMs06(get("/api/v1/bei/eventos/EVT-1/brigadistas"))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventoId").value("EVT-1"))
                .andExpect(jsonPath("$.brigadistas[0].nombre").value("Ana"))
                .andExpect(jsonPath("$.brigadistas[0].zonaId").value("ZONA-1"));
    }

    @Test
    void consultarIndicadoresDebeRetornarLosTotales() throws Exception {
        when(indicadoresService.consultar("EVT-1")).thenReturn(
                new IndicadoresEventoResponse(
                        "EVT-1",
                        OffsetDateTime.parse("2026-11-09T20:00:00-05:00"),
                        5L,
                        3L,
                        2L,
                        List.of(new BrigadistaIndicadorDTO("BRIG-001", 3L, 2L)),
                        List.of()
                )
        );

        mockMvc.perform(
                comoServicioMs06(get("/api/v1/bei/eventos/EVT-1/indicadores"))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncidentes").value(5))
                .andExpect(jsonPath("$.totalCasosAtendidos").value(3))
                .andExpect(jsonPath("$.totalCasosResueltos").value(2))
                .andExpect(jsonPath("$.porBrigadista[0].brigadistaId")
                        .value("BRIG-001"));
    }

    @Test
    void sinCredencialesDebeRetornar401() throws Exception {
        mockMvc.perform(get("/api/v1/bei/eventos/EVT-1/indicadores"))
                .andExpect(status().isUnauthorized());
    }
}