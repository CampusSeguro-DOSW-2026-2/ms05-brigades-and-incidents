package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.AutoasignacionEventoDTO;
import edu.eci.dosw.brigades_and_incidents.dto.AutoasignacionResponse;
import edu.eci.dosw.brigades_and_incidents.dto.BrigadistaGestionDTO;
import edu.eci.dosw.brigades_and_incidents.dto.EdificioEventoDTO;
import edu.eci.dosw.brigades_and_incidents.dto.EventoOperacionDTO;
import edu.eci.dosw.brigades_and_incidents.dto.GestionBrigadistasEventoResponse;
import edu.eci.dosw.brigades_and_incidents.security.SecurityConfig;
import edu.eci.dosw.brigades_and_incidents.service.AlcanceGestion;
import edu.eci.dosw.brigades_and_incidents.service.OperacionBrigadistasService;
import edu.eci.dosw.brigades_and_incidents.support.PermisosPrueba;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(OperacionBrigadistasController.class)
class OperacionBrigadistasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OperacionBrigadistasService service;

    private final EventoOperacionDTO evento = new EventoOperacionDTO(
            "EVT-1", "Evacuación", false, "EN_EJECUCION", true,
            List.of(new EdificioEventoDTO("EDIF-A", "Bloque A", "EDIFICIO", 1))
    );

    private MockHttpServletRequestBuilder con(MockHttpServletRequestBuilder solicitud, String usuario, String rol) {
        return solicitud
                .header("X-User-Id", usuario)
                .header("X-User-Role", rol)
                .header("X-User-Permission", PermisosPrueba.cabecera(rol));
    }

    @Test
    void coordinadorConsultaEventosAbiertos() throws Exception {
        when(service.consultarEventosAbiertos(AlcanceGestion.desdeRoles("COORDINADOR"))).thenReturn(List.of(evento));

        mockMvc.perform(con(get("/api/v1/brigadistas/eventos"), "C1", "COORDINADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventoId").value("EVT-1"))
                .andExpect(jsonPath("$[0].edificios[0].nombre").value("Bloque A"));
    }

    @Test
    void coordinadorConsultaPanelDeGestion() throws Exception {
        when(service.consultarGestion("EVT-1", AlcanceGestion.desdeRoles("COORDINADOR"))).thenReturn(new GestionBrigadistasEventoResponse(evento, List.of(
                new BrigadistaGestionDTO("B1", "Ana", "ACTIVO", "A1", "EDIF-A", "Bloque A", 1, 2)
        )));

        mockMvc.perform(con(get("/api/v1/brigadistas/eventos/EVT-1/gestion"), "C1", "COORDINADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brigadistas[0].nombre").value("Ana"))
                .andExpect(jsonPath("$.brigadistas[0].incidentesResueltos").value(2));
    }

    @Test
    void panelDeEventoDeOtroTipoResponde403() throws Exception {
        when(service.consultarGestion("SIM-1", AlcanceGestion.desdeRoles("COORDINADOR")))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("simulacro"));

        mockMvc.perform(con(get("/api/v1/brigadistas/eventos/SIM-1/gestion"), "C1", "COORDINADOR"))
                .andExpect(status().isForbidden());
    }

    @Test
    void brigadistaNoVeElPanelDeGestion() throws Exception {
        mockMvc.perform(con(get("/api/v1/brigadistas/eventos/EVT-1/gestion"), "B1", "BRIGADISTA"))
                .andExpect(status().isForbidden());
    }

    @Test
    void coordinadorCambiaAsignacion() throws Exception {
        when(service.cambiarAsignacion("EVT-1", "B1", "EDIF-A", "C1", AlcanceGestion.desdeRoles("COORDINADOR"))).thenReturn(
                new BrigadistaGestionDTO("B1", "B1", "ACTIVO", "A1", "EDIF-A", "Bloque A", 0, 0));

        mockMvc.perform(con(put("/api/v1/brigadistas/eventos/EVT-1/brigadistas/B1/asignacion"), "C1", "COORDINADOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ubicacionId\":\"EDIF-A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ubicacionId").value("EDIF-A"));
    }

    @Test
    void cambioSinEdificioResponde400() throws Exception {
        mockMvc.perform(con(put("/api/v1/brigadistas/eventos/EVT-1/brigadistas/B1/asignacion"), "C1", "COORDINADOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ubicacionId\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void brigadistaConsultaSuAutoasignacion() throws Exception {
        when(service.consultarAutoasignacion("B1")).thenReturn(new AutoasignacionResponse("B1", "INACTIVO",
                List.of(new AutoasignacionEventoDTO(evento, null, null, null))));

        mockMvc.perform(con(get("/api/v1/brigadistas/autoasignacion"), "B1", "BRIGADISTA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"))
                .andExpect(jsonPath("$.eventos[0].evento.nombre").value("Evacuación"));
    }

    @Test
    void brigadistaSeAutoasigna() throws Exception {
        when(service.autoasignarse("EVT-1", "B1", "EDIF-A"))
                .thenReturn(new AutoasignacionEventoDTO(evento, "A1", "EDIF-A", "Bloque A"));

        mockMvc.perform(con(put("/api/v1/brigadistas/autoasignacion/eventos/EVT-1"), "B1", "BRIGADISTA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ubicacionId\":\"EDIF-A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUbicacion").value("Bloque A"));
    }

    @Test
    void brigadistaRetiraSuAutoasignacion() throws Exception {
        when(service.retirarAutoasignacion("EVT-1", "B1"))
                .thenReturn(new AutoasignacionEventoDTO(evento, null, null, null));

        mockMvc.perform(con(delete("/api/v1/brigadistas/autoasignacion/eventos/EVT-1"), "B1", "BRIGADISTA"))
                .andExpect(status().isOk());

        verify(service).retirarAutoasignacion(eq("EVT-1"), eq("B1"));
    }

    @Test
    void coordinadorNoUsaLaAutoasignacion() throws Exception {
        mockMvc.perform(con(get("/api/v1/brigadistas/autoasignacion"), "C1", "COORDINADOR"))
                .andExpect(status().isForbidden());
    }
}
