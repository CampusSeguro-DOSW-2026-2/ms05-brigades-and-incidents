
package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.entity.RegistroAuditoria;
import edu.eci.dosw.brigades_and_incidents.entity.TipoRegistroAuditoria;
import edu.eci.dosw.brigades_and_incidents.security.SecurityConfig;
import edu.eci.dosw.brigades_and_incidents.service.RegistroAuditoriaService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(RegistroAuditoriaController.class)
class RegistroAuditoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistroAuditoriaService auditoriaService;

    private MockHttpServletRequestBuilder solicitudAuditor(String url) {
        return get(url)
                .header("X-User-Id", "auditor-123")
                .header("X-User-Role", "AUDITOR");
    }

    private RegistroAuditoria crearRegistro() {
        TipoRegistroAuditoria tipo = new TipoRegistroAuditoria();
        tipo.setNombre("INCIDENTE");

        RegistroAuditoria registro = new RegistroAuditoria();
        registro.setId(UUID.fromString(
                "123e4567-e89b-12d3-a456-426614174000"
        ));
        registro.setTipoRegistro(tipo);
        registro.setUsuarioOrigen("usuario-123");
        registro.setAccion("CREAR_INCIDENTE");
        registro.setRecursoTipo("INCIDENTE");
        registro.setRecursoId("incidente-123");
        registro.setFechaHoraRegistro(
                OffsetDateTime.parse("2026-11-09T20:00:00-05:00")
        );
        registro.setEventoId("evento-123");
        registro.setOrigen("MS05");
        registro.setResultado("EXITOSO");
        registro.setDetalle("Incidente creado correctamente");

        return registro;
    }

    @Test
    void consultarTodosDebeRetornarListaVacia() throws Exception {
        when(auditoriaService.consultarTodos())
                .thenReturn(List.of());

        mockMvc.perform(solicitudAuditor("/api/v1/bei/auditoria"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void consultarTodosDebeRetornarRegistros() throws Exception {
        when(auditoriaService.consultarTodos())
                .thenReturn(List.of(crearRegistro()));

        mockMvc.perform(solicitudAuditor("/api/v1/bei/auditoria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoRegistro").value("INCIDENTE"))
                .andExpect(jsonPath("$[0].usuarioOrigen").value("usuario-123"))
                .andExpect(jsonPath("$[0].accion").value("CREAR_INCIDENTE"))
                .andExpect(jsonPath("$[0].recursoId").value("incidente-123"))
                .andExpect(jsonPath("$[0].resultado").value("EXITOSO"));
    }

    @Test
    void consultarPorEventoDebeRetornarListaVacia() throws Exception {
        when(auditoriaService.consultarPorEvento("evento-123"))
                .thenReturn(List.of());

        mockMvc.perform(
                solicitudAuditor("/api/v1/bei/auditoria/eventos/evento-123")
        )
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void consultarPorEventoDebeRetornarRegistros() throws Exception {
        when(auditoriaService.consultarPorEvento("evento-123"))
                .thenReturn(List.of(crearRegistro()));

        mockMvc.perform(
                solicitudAuditor("/api/v1/bei/auditoria/eventos/evento-123")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventoId").value("evento-123"))
                .andExpect(jsonPath("$[0].accion").value("CREAR_INCIDENTE"));
    }

    @Test
    void consultarPorRecursoDebeRetornarListaVacia() throws Exception {
        when(auditoriaService.consultarPorRecurso(
                "INCIDENTE", "incidente-123"
        )).thenReturn(List.of());

        mockMvc.perform(
                solicitudAuditor(
                        "/api/v1/bei/auditoria/recursos/INCIDENTE/incidente-123"
                )
        )
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void consultarPorRecursoDebeRetornarRegistros() throws Exception {
        when(auditoriaService.consultarPorRecurso(
                "INCIDENTE", "incidente-123"
        )).thenReturn(List.of(crearRegistro()));

        mockMvc.perform(
                solicitudAuditor(
                        "/api/v1/bei/auditoria/recursos/INCIDENTE/incidente-123"
                )
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recursoTipo").value("INCIDENTE"))
                .andExpect(jsonPath("$[0].recursoId").value("incidente-123"))
                .andExpect(jsonPath("$[0].origen").value("MS05"));
    }
}
