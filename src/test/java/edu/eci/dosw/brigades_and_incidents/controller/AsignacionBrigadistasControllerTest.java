package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.AsignacionResponse;
import edu.eci.dosw.brigades_and_incidents.dto.CrearAsignacionRequest;
import edu.eci.dosw.brigades_and_incidents.dto.ModificarAsignacionRequest;
import edu.eci.dosw.brigades_and_incidents.security.SecurityConfig;
import edu.eci.dosw.brigades_and_incidents.service.AsignacionBrigadistasService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(AsignacionBrigadistasController.class)
class AsignacionBrigadistasControllerTest {

    private static final UUID ID =
            UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    private static final String JSON_ASIGNACION = """
            {"brigadistaId":"BRIG-001","eventoId":"EVT-1",
             "ubicacionId":"EDIF-001","tipoUbicacion":"EDIFICIO",
             "observaciones":"obs"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AsignacionBrigadistasService service;

    private AsignacionResponse respuesta() {
        return new AsignacionResponse(
                ID, "BRIG-001", "EVT-1", "EDIF-001", "EDIFICIO",
                "VIGENTE", "obs",
                LocalDateTime.of(2026, 11, 9, 20, 0),
                LocalDateTime.of(2026, 11, 9, 20, 5)
        );
    }

    private MockHttpServletRequestBuilder con(
            MockHttpServletRequestBuilder solicitud,
            String usuarioId,
            String rol
    ) {
        return solicitud
                .header("X-User-Id", usuarioId)
                .header("X-User-Role", rol);
    }

    @Test
    void coordinadorCreaAsignacionYRecibe201() throws Exception {
        when(service.crearAsignacion(
                any(CrearAsignacionRequest.class), eq("COORD-1")))
                .thenReturn(respuesta());

        mockMvc.perform(
                con(post("/api/v1/asignaciones"), "COORD-1", "COORDINADOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_ASIGNACION)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.brigadistaId").value("BRIG-001"))
                .andExpect(jsonPath("$.estado").value("VIGENTE"));
    }

    @Test
    void crearAsignacionConCuerpoInvalidoRecibe400() throws Exception {
        mockMvc.perform(
                con(post("/api/v1/asignaciones"), "COORD-1", "COORDINADOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"brigadistaId":"","eventoId":"EVT-1",
                                 "ubicacionId":"EDIF-001",
                                 "tipoUbicacion":"EDIFICIO"}
                                """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    void brigadistaSeAutoasignaConSuPropioIdentificador() throws Exception {
        when(service.crearAsignacion(
                any(CrearAsignacionRequest.class), eq("BRIG-001")))
                .thenReturn(respuesta());

        // El cuerpo dice "OTRO", pero debe prevalecer el id del header.
        mockMvc.perform(
                con(post("/api/v1/asignaciones/mis-asignaciones"),
                        "BRIG-001", "BRIGADISTA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"brigadistaId":"OTRO","eventoId":"EVT-1",
                                 "ubicacionId":"EDIF-001",
                                 "tipoUbicacion":"EDIFICIO"}
                                """)
        )
                .andExpect(status().isCreated());

        ArgumentCaptor<CrearAsignacionRequest> captor =
                ArgumentCaptor.forClass(CrearAsignacionRequest.class);
        verify(service).crearAsignacion(captor.capture(), eq("BRIG-001"));
        assertThat(captor.getValue().brigadistaId()).isEqualTo("BRIG-001");
    }

    @Test
    void coordinadorModificaAsignacion() throws Exception {
        when(service.modificarAsignacion(
                eq(ID), any(ModificarAsignacionRequest.class), eq("COORD-1")))
                .thenReturn(respuesta());

        mockMvc.perform(
                con(put("/api/v1/asignaciones/" + ID),
                        "COORD-1", "COORDINADOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ubicacionId":"ZONA-001",
                                 "tipoUbicacion":"ZONA_SEGURA",
                                 "observaciones":"cambio"}
                                """)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventoId").value("EVT-1"));
    }

    @Test
    void coordinadorRetiraAsignacion() throws Exception {
        when(service.retirarAsignacion(ID, "COORD-1"))
                .thenReturn(respuesta());

        mockMvc.perform(
                con(patch("/api/v1/asignaciones/" + ID + "/retiro"),
                        "COORD-1", "COORDINADOR")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idAsignacion").value(ID.toString()));
    }

    @Test
    void ms06CreaAsignacionesPredeterminadas() throws Exception {
        when(service.crearAsignacionesPorDefecto(anyList(), eq("ms06")))
                .thenReturn(List.of(respuesta()));

        mockMvc.perform(
                post("/api/v1/asignaciones/predeterminadas")
                        .header("X-Service-Name", "ms06")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + JSON_ASIGNACION + "]")
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].brigadistaId").value("BRIG-001"));
    }

    @Test
    void auditorConsultaUnaAsignacion() throws Exception {
        when(service.consultarAsignacion(ID)).thenReturn(respuesta());

        mockMvc.perform(
                con(get("/api/v1/asignaciones/" + ID), "AUD-1", "AUDITOR")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ubicacionId").value("EDIF-001"));
    }

    @Test
    void coordinadorConsultaAsignacionesPorEvento() throws Exception {
        when(service.consultarPorEvento("EVT-1"))
                .thenReturn(List.of(respuesta()));

        mockMvc.perform(
                con(get("/api/v1/asignaciones/evento/EVT-1"),
                        "COORD-1", "COORDINADOR")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventoId").value("EVT-1"));
    }

    @Test
    void brigadistaConsultaSusPropiasAsignaciones() throws Exception {
        when(service.consultarPorBrigadista("BRIG-001"))
                .thenReturn(List.of(respuesta()));

        mockMvc.perform(
                con(get("/api/v1/asignaciones/brigadista/BRIG-001"),
                        "BRIG-001", "BRIGADISTA")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].brigadistaId").value("BRIG-001"));
    }

    @Test
    void sinCredencialesRecibe401() throws Exception {
        mockMvc.perform(get("/api/v1/asignaciones/evento/EVT-1"))
                .andExpect(status().isUnauthorized());
    }
}