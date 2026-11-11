package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.security.SecurityConfig;
import edu.eci.dosw.brigades_and_incidents.service.ActivarBrigadistaService;
import edu.eci.dosw.brigades_and_incidents.service.AsignacionBrigadistasService;
import edu.eci.dosw.brigades_and_incidents.service.BuscarBrigadistaService;
import edu.eci.dosw.brigades_and_incidents.service.ConsultaResponsabilidadesService;
import edu.eci.dosw.brigades_and_incidents.service.EvidenciaIncidenteService;
import edu.eci.dosw.brigades_and_incidents.service.GestionAtencionIncidenteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reglas del equipo de MS05: el coordinador y el organizador asignan brigadistas y monitorean; cualquiera menos
 * el auditor crea incidentes y adjunta sus evidencias (el servicio exige ser el dueño o un brigadista).
 */
@Import(SecurityConfig.class)
@WebMvcTest({
        AsignacionBrigadistasController.class,
        BuscarBrigadistaController.class,
        ActivacionBrigadistaController.class,
        ResponsabilidadesController.class,
        GestionAtencionIncidenteController.class,
        EvidenciaIncidenteController.class
})
class PermisosOrganizadorYEvidenciasTest {

    private static final String EVENTO = "EVT-1";
    private static final UUID INCIDENTE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String BASE_INCIDENTE = "/api/v1/incidentes/eventos/" + EVENTO + "/" + INCIDENTE;
    private static final String JSON_ASIGNACION = """
            {"brigadistaId":"BRIG-001","eventoId":"EVT-1",
             "ubicacionId":"EDIF-001","tipoUbicacion":"EDIFICIO"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AsignacionBrigadistasService asignacionService;
    @MockitoBean
    private BuscarBrigadistaService buscarBrigadistaService;
    @MockitoBean
    private ActivarBrigadistaService activarBrigadistaService;
    @MockitoBean
    private ConsultaResponsabilidadesService responsabilidadesService;
    @MockitoBean
    private GestionAtencionIncidenteService gestionService;
    @MockitoBean
    private EvidenciaIncidenteService evidenciaService;

    private static <B extends AbstractMockHttpServletRequestBuilder<B>> B como(B solicitud, String roles) {
        return solicitud.header("X-User-Id", "usuario-1").header("X-User-Role", roles);
    }

    @Test
    void organizadorAsignaBrigadistas() throws Exception {
        mockMvc.perform(como(post("/api/v1/asignaciones"), "ORGANIZADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_ASIGNACION))
                .andExpect(status().isCreated());
    }

    @Test
    void organizadorRetiraUnaAsignacion() throws Exception {
        mockMvc.perform(como(patch("/api/v1/asignaciones/" + INCIDENTE + "/retiro"), "ORGANIZADOR"))
                .andExpect(status().isOk());
    }

    @Test
    void organizadorAsignaElResponsableDeUnIncidente() throws Exception {
        mockMvc.perform(como(patch(BASE_INCIDENTE + "/responsable"), "ORGANIZADOR")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"responsableId\":\"BRIG-001\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void organizadorBuscaBrigadistas() throws Exception {
        mockMvc.perform(como(get("/api/v1/brigadistas/buscar").param("nombreBrigadista", "Ana"), "ORGANIZADOR"))
                .andExpect(status().isOk());
    }

    @Test
    void organizadorMonitoreaAsignacionesResponsabilidadesHistorialYEvidencias() throws Exception {
        mockMvc.perform(como(get("/api/v1/asignaciones/evento/" + EVENTO), "ORGANIZADOR"))
                .andExpect(status().isOk());
        mockMvc.perform(como(get("/api/v1/responsabilidades"), "ORGANIZADOR"))
                .andExpect(status().isOk());
        mockMvc.perform(como(get(BASE_INCIDENTE + "/historial"), "ORGANIZADOR"))
                .andExpect(status().isOk());
        mockMvc.perform(como(get(BASE_INCIDENTE + "/evidencias"), "ORGANIZADOR"))
                .andExpect(status().isOk());
    }

    @Test
    void cerrarUnIncidenteSigueSiendoSoloDelCoordinador() throws Exception {
        mockMvc.perform(como(post(BASE_INCIDENTE + "/cerrar"), "ORGANIZADOR"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elEstudianteYElAuditorNoAsignanBrigadistas() throws Exception {
        mockMvc.perform(como(post("/api/v1/asignaciones"), "ESTUDIANTE")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_ASIGNACION))
                .andExpect(status().isForbidden());
        mockMvc.perform(como(post("/api/v1/asignaciones"), "AUDITOR")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_ASIGNACION))
                .andExpect(status().isForbidden());
    }

    @Test
    void quienCreaIncidentesAdjuntaEvidenciasYElAuditorNo() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "foto.png", "image/png", new byte[] {1});
        for (String rol : new String[] {"ESTUDIANTE", "PROFESOR", "ADMINISTRATIVO", "BRIGADISTA", "ORGANIZADOR"}) {
            mockMvc.perform(como(multipart(BASE_INCIDENTE + "/evidencias").file(archivo), rol))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(como(multipart(BASE_INCIDENTE + "/evidencias").file(archivo), "AUDITOR"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elBrigadistaCambiaSuPropioEstadoPeroNoElDeOtro() throws Exception {
        mockMvc.perform(como(put("/api/v1/brigadistas/usuario-1/estado"), "BRIGADISTA")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ACTIVO\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(como(put("/api/v1/brigadistas/otro-brigadista/estado"), "BRIGADISTA")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ACTIVO\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(como(put("/api/v1/brigadistas/otro-brigadista/estado"), "ORGANIZADOR")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ACTIVO\"}"))
                .andExpect(status().isOk());
    }
}
