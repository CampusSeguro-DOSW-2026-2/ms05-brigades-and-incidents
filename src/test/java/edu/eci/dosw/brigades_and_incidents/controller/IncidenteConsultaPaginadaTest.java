package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.ConsultaIncidentesRequest;
import edu.eci.dosw.brigades_and_incidents.dto.IncidenteResumenDTO;
import edu.eci.dosw.brigades_and_incidents.security.SecurityConfig;
import edu.eci.dosw.brigades_and_incidents.service.ConsultaIncidentesService;
import edu.eci.dosw.brigades_and_incidents.service.IncidenteService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El listado de incidentes usa la paginación de los Estándares (§9): page/size y contenido, total_elementos. */
@Import(SecurityConfig.class)
@WebMvcTest(IncidenteController.class)
class IncidenteConsultaPaginadaTest {

    private static final String URL = "/api/v1/incidentes/eventos/EVT-1/consulta";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidenteService incidenteService;
    @MockitoBean
    private ConsultaIncidentesService consultaService;

    @Test
    void respondeConElFormatoDePaginacionEstandar() throws Exception {
        // Página 1 de tamaño 5, completa, de 12 incidentes en total (3 páginas).
        List<IncidenteResumenDTO> incidentes = List.of(mock(IncidenteResumenDTO.class), mock(IncidenteResumenDTO.class),
                mock(IncidenteResumenDTO.class), mock(IncidenteResumenDTO.class), mock(IncidenteResumenDTO.class));
        when(consultaService.consultarIncidentes(eq("EVT-1"), any(ConsultaIncidentesRequest.class), eq("usuario-1")))
                .thenReturn(new PageImpl<>(incidentes, PageRequest.of(1, 5), 12));

        mockMvc.perform(get(URL).param("page", "1").param("size", "5")
                        .header("X-User-Id", "usuario-1").header("X-User-Role", "COORDINADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(5))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.total_elementos").value(12))
                .andExpect(jsonPath("$.total_paginas").value(3))
                .andExpect(jsonPath("$.content").doesNotExist());

        ArgumentCaptor<ConsultaIncidentesRequest> filtros = ArgumentCaptor.forClass(ConsultaIncidentesRequest.class);
        verify(consultaService).consultarIncidentes(eq("EVT-1"), filtros.capture(), eq("usuario-1"));
        assertThat(filtros.getValue().pagina()).isEqualTo(1);
        assertThat(filtros.getValue().tamanio()).isEqualTo(5);
    }

    @Test
    void siguenFuncionandoLosParametrosPaginaYTamanio() throws Exception {
        when(consultaService.consultarIncidentes(eq("EVT-1"), any(ConsultaIncidentesRequest.class), eq("usuario-1")))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 3), 0));

        mockMvc.perform(get(URL).param("pagina", "2").param("tamanio", "3")
                        .header("X-User-Id", "usuario-1").header("X-User-Role", "BRIGADISTA"))
                .andExpect(status().isOk());

        ArgumentCaptor<ConsultaIncidentesRequest> filtros = ArgumentCaptor.forClass(ConsultaIncidentesRequest.class);
        verify(consultaService).consultarIncidentes(eq("EVT-1"), filtros.capture(), eq("usuario-1"));
        assertThat(filtros.getValue().pagina()).isEqualTo(2);
        assertThat(filtros.getValue().tamanio()).isEqualTo(3);
    }
}
