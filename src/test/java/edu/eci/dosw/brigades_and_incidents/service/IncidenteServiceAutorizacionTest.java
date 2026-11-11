
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.client.UbicacionClient;
import edu.eci.dosw.brigades_and_incidents.client.UsuarioClient;
import edu.eci.dosw.brigades_and_incidents.dto.CrearIncidenteRequest;
import edu.eci.dosw.brigades_and_incidents.event.IncidenteAuditPublisher;
import edu.eci.dosw.brigades_and_incidents.mapper.IncidenteMapper;
import edu.eci.dosw.brigades_and_incidents.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidenteServiceAutorizacionTest {

    @Mock
    private IncidenteRepository incidenteRepository;

    @Mock
    private HistorialIncidenteRepository historialRepository;

    @Mock
    private PrioridadIncidenteRepository prioridadRepository;

    @Mock
    private EstadoIncidenteRepository estadoRepository;

    @Mock
    private TipoUbicacionRepository tipoUbicacionRepository;

    @Mock
    private AsignacionBrigadistaRepository asignacionRepository;

    @Mock
    private BrigadistaRepository brigadistaRepository;

    @Mock
    private GestionOperacionEventoService gestionEventoService;

    @Mock
    private UbicacionClient ubicacionClient;

    @Mock
    private UsuarioClient usuarioClient;

    @Mock
    private IncidenteMapper incidenteMapper;

    @Mock
    private IncidenteAuditPublisher auditPublisher;

    private IncidenteService incidenteService;

    private static final String USUARIO_ID =
            "11111111-1111-1111-1111-111111111111";

    private static final String EVENTO_ID = "EVENTO-001";

    @BeforeEach
    void configurar() {
        incidenteService = new IncidenteService(
                incidenteRepository,
                historialRepository,
                prioridadRepository,
                estadoRepository,
                tipoUbicacionRepository,
                asignacionRepository,
                brigadistaRepository,
                gestionEventoService,
                ubicacionClient,
                usuarioClient,
                incidenteMapper,
                auditPublisher
        );
    }

    private CrearIncidenteRequest crearSolicitudPropia() {
        return new CrearIncidenteRequest(
                null,
                1L,
                "EDIF-001",
                "EDIFICIO",
                "Solicitud de ayuda"
        );
    }

    private CrearIncidenteRequest crearSolicitudParaTercero() {
        return new CrearIncidenteRequest(
                "Persona afectada",
                1L,
                "EDIF-001",
                "EDIFICIO",
                "Solicitud de ayuda"
        );
    }

    private UsuarioClient.UsuarioExterno crearUsuario(
            String nombre,
            List<String> roles,
            String estado
    ) {
        return new UsuarioClient.UsuarioExterno(
                USUARIO_ID,
                nombre,
                roles,
                estado
        );
    }

    // --------------------------------------------------
    // IDENTIDAD
    // --------------------------------------------------

    @Test
    void usuarioSinIdentidadNoPuedeRegistrarIncidente() {
        assertThrows(
                ResponseStatusException.class,
                () -> incidenteService.registrarIncidente(
                        EVENTO_ID,
                        crearSolicitudPropia(),
                        null
                )
        );

        verifyNoInteractions(usuarioClient);
    }

    // --------------------------------------------------
    // VALIDACIÓN DEL USUARIO EN MS01
    // --------------------------------------------------

    @Test
    void usuarioInexistenteEnMs01NoPuedeRegistrarIncidente() {
        when(usuarioClient.consultarUsuario(USUARIO_ID))
                .thenReturn(null);

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> incidenteService.registrarIncidente(
                        EVENTO_ID,
                        crearSolicitudPropia(),
                        USUARIO_ID
                )
        );

        assertEquals(403, excepcion.getStatusCode().value());
    }

    @Test
    void usuarioInactivoNoPuedeRegistrarIncidente() {
        when(usuarioClient.consultarUsuario(USUARIO_ID))
                .thenReturn(crearUsuario(
                        "Usuario Inactivo",
                        List.of("ESTUDIANTE"),
                        "INACTIVO"
                ));

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> incidenteService.registrarIncidente(
                        EVENTO_ID,
                        crearSolicitudPropia(),
                        USUARIO_ID
                )
        );

        assertEquals(403, excepcion.getStatusCode().value());
    }

    // --------------------------------------------------
    // ROLES NO AUTORIZADOS
    // --------------------------------------------------

    @Test
    void auditorNoPuedeRegistrarIncidentes() {
        when(usuarioClient.consultarUsuario(USUARIO_ID))
                .thenReturn(crearUsuario(
                        "Usuario Auditor",
                        List.of("AUDITOR"),
                        "ACTIVO"
                ));

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> incidenteService.registrarIncidente(
                        EVENTO_ID,
                        crearSolicitudPropia(),
                        USUARIO_ID
                )
        );

        assertEquals(403, excepcion.getStatusCode().value());
        verifyNoInteractions(gestionEventoService);
    }

    @Test
    void rolDesconocidoNoPuedeRegistrarIncidentes() {
        when(usuarioClient.consultarUsuario(USUARIO_ID))
                .thenReturn(crearUsuario(
                        "Usuario sin permisos",
                        List.of("ROL_DESCONOCIDO"),
                        "ACTIVO"
                ));

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> incidenteService.registrarIncidente(
                        EVENTO_ID,
                        crearSolicitudPropia(),
                        USUARIO_ID
                )
        );

        assertEquals(403, excepcion.getStatusCode().value());
    }

    @Test
    void usuarioConRolBrigadistaYAuditorNoPuedeRegistrar() {
        when(usuarioClient.consultarUsuario(USUARIO_ID))
                .thenReturn(crearUsuario(
                        "Usuario con dos roles",
                        List.of("BRIGADISTA", "AUDITOR"),
                        "ACTIVO"
                ));

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> incidenteService.registrarIncidente(
                        EVENTO_ID,
                        crearSolicitudPropia(),
                        USUARIO_ID
                )
        );

        assertEquals(403, excepcion.getStatusCode().value());
    }

    // --------------------------------------------------
    // ROLES DE USUARIO GENERAL AUTORIZADOS
    // --------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
            "ESTUDIANTE",
            "PROFESOR",
            "ADMINISTRATIVO",
            "COORDINADOR",
            "ORGANIZADOR",
            "ADMIN"
    })
    void usuarioGeneralConRolPermitidoPuedeIniciarRegistro(
            String rol
    ) {
        when(usuarioClient.consultarUsuario(USUARIO_ID))
                .thenReturn(crearUsuario(
                        "Usuario de prueba",
                        List.of(rol),
                        "ACTIVO"
                ));

        assertThrows(
                IllegalStateException.class,
                () -> incidenteService.registrarIncidente(
                        EVENTO_ID,
                        crearSolicitudPropia(),
                        USUARIO_ID
                )
        );

        verify(gestionEventoService).obtenerConfiguracion(EVENTO_ID);
    }

    // --------------------------------------------------
    // BRIGADISTAS
    // --------------------------------------------------

    @Test
    void brigadistaActivoEnMs01PuedeIniciarRegistroPropio() {
        when(usuarioClient.consultarUsuario(USUARIO_ID))
                .thenReturn(crearUsuario(
                        "Paula Novoa",
                        List.of("BRIGADISTA"),
                        "ACTIVO"
                ));

        assertThrows(
                IllegalStateException.class,
                () -> incidenteService.registrarIncidente(
                        EVENTO_ID,
                        crearSolicitudPropia(),
                        USUARIO_ID
                )
        );

        verify(gestionEventoService).obtenerConfiguracion(EVENTO_ID);
        verifyNoInteractions(brigadistaRepository);
    }

    @Test
    void brigadistaSinRegistroEnMs05NoPuedeRegistrarParaTercero() {
        when(usuarioClient.consultarUsuario(USUARIO_ID))
                .thenReturn(crearUsuario(
                        "Paula Novoa",
                        List.of("BRIGADISTA"),
                        "ACTIVO"
                ));

        when(brigadistaRepository.findByUsuarioIdExterno(USUARIO_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> incidenteService.registrarIncidente(
                        EVENTO_ID,
                        crearSolicitudParaTercero(),
                        USUARIO_ID
                )
        );

        assertEquals(403, excepcion.getStatusCode().value());

        verify(brigadistaRepository)
                .findByUsuarioIdExterno(USUARIO_ID);

        verifyNoInteractions(gestionEventoService);
    }
}
