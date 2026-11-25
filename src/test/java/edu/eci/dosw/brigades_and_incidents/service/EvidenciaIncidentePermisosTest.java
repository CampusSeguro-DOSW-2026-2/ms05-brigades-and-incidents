package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.client.UsuarioClient;
import edu.eci.dosw.brigades_and_incidents.entity.EstadoIncidente;
import edu.eci.dosw.brigades_and_incidents.entity.Incidente;
import edu.eci.dosw.brigades_and_incidents.event.IncidenteAuditPublisher;
import edu.eci.dosw.brigades_and_incidents.repository.BrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.EvidenciaIncidenteRepository;
import edu.eci.dosw.brigades_and_incidents.repository.IncidenteRepository;
import edu.eci.dosw.brigades_and_incidents.support.PermisosPrueba;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** La autorización de las evidencias depende de los permisos de X-User-Permission, no de los roles de MS01. */
class EvidenciaIncidentePermisosTest {

    private static final String EVENTO_ID = "EVT-1";
    private static final String DUENO_ID = "dueno-1";
    private static final String OTRO_ID = "otro-1";
    private static final UUID INCIDENTE_ID = UUID.randomUUID();

    private EvidenciaIncidenteService service;
    private UsuarioClient usuarioClient;
    private EvidenciaIncidenteRepository evidenciaRepository;

    @BeforeEach
    void configurar() {
        evidenciaRepository = mock(EvidenciaIncidenteRepository.class);
        IncidenteRepository incidenteRepository = mock(IncidenteRepository.class);
        usuarioClient = mock(UsuarioClient.class);

        service = new EvidenciaIncidenteService(
                evidenciaRepository,
                incidenteRepository,
                mock(BrigadistaRepository.class),
                mock(EvidenciaStorage.class),
                mock(IncidenteAuditPublisher.class),
                usuarioClient
        );

        Incidente incidente = new Incidente();
        incidente.setId(INCIDENTE_ID);
        incidente.setEventoId(EVENTO_ID);
        incidente.setReportanteId(DUENO_ID);
        incidente.setEstado(new EstadoIncidente("ABIERTO", "Incidente abierto"));
        when(incidenteRepository.findById(INCIDENTE_ID)).thenReturn(Optional.of(incidente));

        when(usuarioClient.consultarUsuario(any())).thenAnswer(invocacion ->
                new UsuarioClient.UsuarioExterno(invocacion.getArgument(0), "Usuario", List.of(), "ACTIVO"));
    }

    @AfterEach
    void limpiar() {
        PermisosPrueba.limpiar();
    }

    private MockMultipartFile archivo() {
        return new MockMultipartFile("archivo", "e.png", "image/png", new byte[] {1});
    }

    private void autenticarConIdentidad(String usuarioId, String roles) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                usuarioId, null,
                PermisosPrueba.de(roles).stream().map(SimpleGrantedAuthority::new).toList()));
    }

    @Test
    void sinElPermisoDeReportarNoSePuedeCargarEvidencia() {
        PermisosPrueba.autenticarComo("AUDITOR");

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.cargarEvidencia(EVENTO_ID, INCIDENTE_ID, archivo(), DUENO_ID));

        assertEquals(403, e.getStatusCode().value());
    }

    @Test
    void quienNoEsDuenoNiBrigadistaNoPuedeCargarEvidencia() {
        PermisosPrueba.autenticarComo("ESTUDIANTE");

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.cargarEvidencia(EVENTO_ID, INCIDENTE_ID, archivo(), OTRO_ID));

        assertEquals(403, e.getStatusCode().value());
    }

    @Test
    void sinElPermisoDeLecturaNoSeListanEvidencias() {
        autenticarConIdentidad(OTRO_ID, "ESTUDIANTE");

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.listarEvidencias(EVENTO_ID, INCIDENTE_ID));

        assertEquals(403, e.getStatusCode().value());
    }

    @Test
    void elAuditorYElCoordinadorListanEvidencias() {
        when(evidenciaRepository.findByIncidenteId(INCIDENTE_ID)).thenReturn(List.of());

        autenticarConIdentidad(OTRO_ID, "AUDITOR");
        assertTrue(service.listarEvidencias(EVENTO_ID, INCIDENTE_ID).isEmpty());

        autenticarConIdentidad(OTRO_ID, "COORDINADOR");
        assertTrue(service.listarEvidencias(EVENTO_ID, INCIDENTE_ID).isEmpty());
    }
}
