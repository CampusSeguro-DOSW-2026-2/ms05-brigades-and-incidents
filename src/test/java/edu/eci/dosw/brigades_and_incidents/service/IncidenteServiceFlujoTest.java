
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.client.UbicacionClient;
import edu.eci.dosw.brigades_and_incidents.client.UsuarioClient;
import edu.eci.dosw.brigades_and_incidents.dto.CrearIncidenteRequest;
import edu.eci.dosw.brigades_and_incidents.dto.IncidenteResponse;
import edu.eci.dosw.brigades_and_incidents.event.IncidenteAuditPublisher;
import edu.eci.dosw.brigades_and_incidents.entity.*;
import edu.eci.dosw.brigades_and_incidents.mapper.IncidenteMapper;
import edu.eci.dosw.brigades_and_incidents.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class IncidenteServiceFlujoTest {

    private IncidenteRepository incidenteRepository;
    private PrioridadIncidenteRepository prioridadRepository;
    private EstadoIncidenteRepository estadoRepository;
    private TipoUbicacionRepository tipoUbicacionRepository;
    private AsignacionBrigadistaRepository asignacionRepository;
    private BrigadistaRepository brigadistaRepository;
    private GestionOperacionEventoService gestionEventoService;
    private UbicacionClient ubicacionClient;
    private UsuarioClient usuarioClient;
    private IncidenteMapper incidenteMapper;
    private IncidenteAuditPublisher auditPublisher;

    private IncidenteService incidenteService;

    private ConfiguracionOperacionEvento configuracionActiva;
    private TipoUbicacion tipoUbicacion;
    private PrioridadIncidente prioridad;
    private EstadoIncidente estadoReportado;
    private EstadoIncidente estadoAsignado;

    private static final String EVENTO_ID = "EVENTO-001";

    private static final String USUARIO_ID =
            "11111111-1111-1111-1111-111111111111";

    private static final String BRIGADISTA_ID =
            "22222222-2222-2222-2222-222222222222";

    @BeforeEach
    void setUp() {

        incidenteRepository = mock(IncidenteRepository.class);
        prioridadRepository = mock(PrioridadIncidenteRepository.class);
        estadoRepository = mock(EstadoIncidenteRepository.class);
        tipoUbicacionRepository = mock(TipoUbicacionRepository.class);
        asignacionRepository = mock(AsignacionBrigadistaRepository.class);
        brigadistaRepository = mock(BrigadistaRepository.class);
        gestionEventoService = mock(GestionOperacionEventoService.class);
        ubicacionClient = mock(UbicacionClient.class);
        usuarioClient = mock(UsuarioClient.class);
        incidenteMapper = mock(IncidenteMapper.class);
        auditPublisher = mock(IncidenteAuditPublisher.class);

        incidenteService = new IncidenteService(
                incidenteRepository,
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

        // Configuración de un evento activo.
        configuracionActiva = mock(ConfiguracionOperacionEvento.class);

        when(configuracionActiva.isPanelHabilitado()).thenReturn(true);

        when(gestionEventoService.obtenerConfiguracion(EVENTO_ID))
                .thenReturn(configuracionActiva);

        // Catálogos necesarios para registrar el incidente.
        tipoUbicacion = mock(TipoUbicacion.class);
        prioridad = mock(PrioridadIncidente.class);
        estadoReportado = mock(EstadoIncidente.class);
        estadoAsignado = mock(EstadoIncidente.class);

        when(tipoUbicacionRepository.findByNombre("EDIFICIO"))
                .thenReturn(Optional.of(tipoUbicacion));

        when(prioridadRepository.findById(1L))
                .thenReturn(Optional.of(prioridad));

        when(estadoRepository.findByNombre("REPORTADO"))
                .thenReturn(Optional.of(estadoReportado));

        when(estadoRepository.findByNombre("ASIGNADO"))
                .thenReturn(Optional.of(estadoAsignado));

        // Ubicación activa consultada en MS02.
        when(ubicacionClient.consultarUbicacion("EDIF-001", "EDIFICIO"))
                .thenReturn(new UbicacionClient.UbicacionExterna(
                        "EDIF-001",
                        "Edificio principal",
                        "EDIFICIO",
                        true
                ));

        // Por defecto, el evento no tiene brigadistas asignados.
        when(asignacionRepository.findByEventoIdAndEstado_Nombre(
                EVENTO_ID,
                "VIGENTE"
        )).thenReturn(List.of());

        // Simula el guardado y asigna un UUID al incidente.
        when(incidenteRepository.save(any(Incidente.class)))
                .thenAnswer(invocation -> {
                    Incidente incidente = invocation.getArgument(0);
                    incidente.setId(UUID.randomUUID());
                    return incidente;
                });
    }

    @Test
    void usuarioGeneralPuedeRegistrarIncidentePropio() {

        UsuarioClient.UsuarioExterno usuario =
                new UsuarioClient.UsuarioExterno(
                        USUARIO_ID,
                        "Daniel García",
                        List.of("ESTUDIANTE"),
                        "ACTIVO"
                );

        when(usuarioClient.consultarUsuario(USUARIO_ID))
                .thenReturn(usuario);

        IncidenteResponse respuestaEsperada =
                mock(IncidenteResponse.class);

        when(incidenteMapper.toResponse(any(Incidente.class)))
                .thenReturn(respuestaEsperada);

        CrearIncidenteRequest request = new CrearIncidenteRequest(
                "Nombre que no debe utilizarse",
                1L,
                "EDIF-001",
                "EDIFICIO",
                "Necesito ayuda en el edificio."
        );

        IncidenteResponse respuesta = incidenteService.registrarIncidente(
                EVENTO_ID,
                request,
                USUARIO_ID
        );

        assertSame(respuestaEsperada, respuesta);

        verify(incidenteRepository).save(any(Incidente.class));
        verify(incidenteMapper).toResponse(any(Incidente.class));

        // Verifica que se publique el evento de auditoría.
        verify(auditPublisher).publicar(
                eq("CREAR_INCIDENTE"),
                eq("INCIDENTE"),
                anyString(),
                eq(EVENTO_ID),
                eq(USUARIO_ID),
                eq("EXITOSO"),
                anyString()
        );

        verify(ubicacionClient).consultarUbicacion(
                "EDIF-001",
                "EDIFICIO"
        );

        verify(estadoRepository).findByNombre("REPORTADO");

        verify(brigadistaRepository, never())
                .findByUsuarioIdExterno(USUARIO_ID);

        Incidente incidenteGuardado = capturarIncidenteGuardado();

        assertNotNull(incidenteGuardado.getId());

        assertEquals(
                "Daniel García",
                incidenteGuardado.getNombreAfectado()
        );

        assertEquals(
                USUARIO_ID,
                incidenteGuardado.getReportanteId()
        );

        assertEquals(
                "EDIF-001",
                incidenteGuardado.getUbicacionId()
        );

        assertSame(prioridad, incidenteGuardado.getPrioridad());
        assertSame(tipoUbicacion, incidenteGuardado.getTipoUbicacion());
        assertSame(estadoReportado, incidenteGuardado.getEstado());

        assertEquals(
                "Necesito ayuda en el edificio.",
                incidenteGuardado.getDescripcion()
        );
    }

    @Test
    void brigadistaActivoPuedeRegistrarIncidenteParaOtraPersona() {

        UsuarioClient.UsuarioExterno usuario =
                new UsuarioClient.UsuarioExterno(
                        BRIGADISTA_ID,
                        "Paula Novoa",
                        List.of("BRIGADISTA"),
                        "ACTIVO"
                );

        when(usuarioClient.consultarUsuario(BRIGADISTA_ID))
                .thenReturn(usuario);

        Brigadista brigadistaActivo = mock(
                Brigadista.class,
                RETURNS_DEEP_STUBS
        );

        when(brigadistaActivo.getEstado().getNombre())
                .thenReturn("ACTIVO");

        when(brigadistaRepository.findByUsuarioIdExterno(BRIGADISTA_ID))
                .thenReturn(Optional.of(brigadistaActivo));

        IncidenteResponse respuestaEsperada =
                mock(IncidenteResponse.class);

        when(incidenteMapper.toResponse(any(Incidente.class)))
                .thenReturn(respuestaEsperada);

        CrearIncidenteRequest request = new CrearIncidenteRequest(
                "Persona afectada",
                1L,
                "EDIF-001",
                "EDIFICIO",
                "La persona necesita asistencia."
        );

        IncidenteResponse respuesta = incidenteService.registrarIncidente(
                EVENTO_ID,
                request,
                BRIGADISTA_ID
        );

        assertSame(respuestaEsperada, respuesta);

        verify(brigadistaRepository)
                .findByUsuarioIdExterno(BRIGADISTA_ID);

        Incidente incidenteGuardado = capturarIncidenteGuardado();

        assertNotNull(incidenteGuardado.getId());

        assertEquals(
                "Persona afectada",
                incidenteGuardado.getNombreAfectado()
        );

        assertEquals(
                BRIGADISTA_ID,
                incidenteGuardado.getReportanteId()
        );

        assertSame(estadoReportado, incidenteGuardado.getEstado());
    }

    @Test
    void noPermiteRegistrarIncidenteCuandoEventoNoEstaActivo() {

        UsuarioClient.UsuarioExterno usuario =
                new UsuarioClient.UsuarioExterno(
                        USUARIO_ID,
                        "Daniel García",
                        List.of("ESTUDIANTE"),
                        "ACTIVO"
                );

        when(usuarioClient.consultarUsuario(USUARIO_ID))
                .thenReturn(usuario);

        when(gestionEventoService.obtenerConfiguracion(EVENTO_ID))
                .thenReturn(null);

        CrearIncidenteRequest request = crearRequestPropio();

        IllegalStateException excepcion = assertThrows(
                IllegalStateException.class,
                () -> incidenteService.registrarIncidente(
                        EVENTO_ID,
                        request,
                        USUARIO_ID
                )
        );

        assertTrue(excepcion.getMessage().contains("evento"));

        verify(ubicacionClient, never())
                .consultarUbicacion(any(), any());

        verify(incidenteRepository, never()).save(any());
    }

    @Test
    void noPermiteRegistrarIncidenteEnUbicacionInactiva() {

        UsuarioClient.UsuarioExterno usuario =
                new UsuarioClient.UsuarioExterno(
                        USUARIO_ID,
                        "Daniel García",
                        List.of("ESTUDIANTE"),
                        "ACTIVO"
                );

        when(usuarioClient.consultarUsuario(USUARIO_ID))
                .thenReturn(usuario);

        when(ubicacionClient.consultarUbicacion("EDIF-001", "EDIFICIO"))
                .thenReturn(new UbicacionClient.UbicacionExterna(
                        "EDIF-001",
                        "Edificio principal",
                        "EDIFICIO",
                        false
                ));

        CrearIncidenteRequest request = crearRequestPropio();

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> incidenteService.registrarIncidente(
                        EVENTO_ID,
                        request,
                        USUARIO_ID
                )
        );

        assertTrue(excepcion.getMessage().contains("ubicación"));

        verify(incidenteRepository, never()).save(any());
    }

    @Test
    void noPermiteRegistrarIncidenteConPrioridadInexistente() {

        UsuarioClient.UsuarioExterno usuario =
                new UsuarioClient.UsuarioExterno(
                        USUARIO_ID,
                        "Daniel García",
                        List.of("ESTUDIANTE"),
                        "ACTIVO"
                );

        when(usuarioClient.consultarUsuario(USUARIO_ID))
                .thenReturn(usuario);

        when(prioridadRepository.findById(99L))
                .thenReturn(Optional.empty());

        CrearIncidenteRequest request = new CrearIncidenteRequest(
                null,
                99L,
                "EDIF-001",
                "EDIFICIO",
                "Necesito ayuda."
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> incidenteService.registrarIncidente(
                        EVENTO_ID,
                        request,
                        USUARIO_ID
                )
        );

        verify(incidenteRepository, never()).save(any());
    }

    @Test
    void registraIncidenteComoAsignadoCuandoHayBrigadistaActivoEnLaUbicacion() {

        UsuarioClient.UsuarioExterno usuario =
                new UsuarioClient.UsuarioExterno(
                        USUARIO_ID,
                        "Daniel García",
                        List.of("ESTUDIANTE"),
                        "ACTIVO"
                );

        when(usuarioClient.consultarUsuario(USUARIO_ID))
                .thenReturn(usuario);

        // Se crea una asignación vigente para el edificio del incidente.
        AsignacionBrigadista asignacion = mock(
                AsignacionBrigadista.class,
                RETURNS_DEEP_STUBS
        );

        when(asignacion.getUbicacionId()).thenReturn("EDIF-001");

        when(asignacion.getTipoUbicacion().getNombre())
                .thenReturn("EDIFICIO");

        when(asignacion.getBrigadistaId())
                .thenReturn(BRIGADISTA_ID);

        when(asignacionRepository.findByEventoIdAndEstado_Nombre(
                EVENTO_ID,
                "VIGENTE"
        )).thenReturn(List.of(asignacion));

        // El brigadista asociado a la asignación está activo.
        Brigadista brigadistaActivo = mock(
                Brigadista.class,
                RETURNS_DEEP_STUBS
        );

        when(brigadistaActivo.getEstado().getNombre())
                .thenReturn("ACTIVO");

        when(brigadistaRepository.findByUsuarioIdExterno(BRIGADISTA_ID))
                .thenReturn(Optional.of(brigadistaActivo));

        when(incidenteMapper.toResponse(any(Incidente.class)))
                .thenReturn(mock(IncidenteResponse.class));

        incidenteService.registrarIncidente(
                EVENTO_ID,
                crearRequestPropio(),
                USUARIO_ID
        );

        Incidente incidenteGuardado = capturarIncidenteGuardado();

        assertNotNull(incidenteGuardado.getId());

        // Si existe un brigadista activo asignado, el estado debe ser ASIGNADO.
        assertSame(
                estadoAsignado,
                incidenteGuardado.getEstado()
        );

        verify(estadoRepository).findByNombre("ASIGNADO");

        verify(brigadistaRepository)
                .findByUsuarioIdExterno(BRIGADISTA_ID);
    }

    private CrearIncidenteRequest crearRequestPropio() {

        return new CrearIncidenteRequest(
                null,
                1L,
                "EDIF-001",
                "EDIFICIO",
                "Solicitud de ayuda."
        );
    }

    private Incidente capturarIncidenteGuardado() {

        org.mockito.ArgumentCaptor<Incidente> captor =
                org.mockito.ArgumentCaptor.forClass(Incidente.class);

        verify(incidenteRepository).save(captor.capture());

        return captor.getValue();
    }
}
