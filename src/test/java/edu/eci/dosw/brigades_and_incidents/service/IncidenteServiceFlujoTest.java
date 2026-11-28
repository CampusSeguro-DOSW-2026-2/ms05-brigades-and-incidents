package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.support.PermisosPrueba;
import org.junit.jupiter.api.AfterEach;
import edu.eci.dosw.brigades_and_incidents.client.UbicacionClient;
import edu.eci.dosw.brigades_and_incidents.client.UsuarioClient;
import edu.eci.dosw.brigades_and_incidents.dto.CrearIncidenteRequest;
import edu.eci.dosw.brigades_and_incidents.dto.EditarIncidenteRequest;
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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class IncidenteServiceFlujoTest {

    private IncidenteRepository incidenteRepository;
    private HistorialIncidenteRepository historialRepository;
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

    @AfterEach
    void limpiarSeguridad() {
        PermisosPrueba.limpiar();
    }

    @BeforeEach
    void setUp() {

        incidenteRepository = mock(IncidenteRepository.class);
        historialRepository = mock(HistorialIncidenteRepository.class);
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

        PermisosPrueba.autenticarComo("ESTUDIANTE");
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

        verify(auditPublisher).publicar(
                eq("CREAR_INCIDENTE"),
                eq("INCIDENTE"),
                anyString(),
                eq(EVENTO_ID),
                eq(USUARIO_ID),
                eq("EXITOSO"),
                anyString(),
                anyMap()
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

    /**
     * MS04 envía la zona como PUNTO_ENCUENTRO y MS05 la guardó como ZONA_SEGURA. La asignación se resuelve por el
     * identificador de la ubicación, no por el tipo, para que el incidente no quede reportado sin responsable.
     */
    @Test
    void asignaResponsableAunqueElTipoDeLaZonaDifieraEntreMS04YMS05() {

        PermisosPrueba.autenticarComo("ESTUDIANTE");

        when(usuarioClient.consultarUsuario(USUARIO_ID))
                .thenReturn(new UsuarioClient.UsuarioExterno(
                        USUARIO_ID,
                        "Daniel García",
                        List.of("ESTUDIANTE"),
                        "ACTIVO"
                ));

        TipoUbicacion tipoPuntoEncuentro = mock(TipoUbicacion.class);

        when(tipoUbicacionRepository.findByNombre("PUNTO_ENCUENTRO"))
                .thenReturn(Optional.of(tipoPuntoEncuentro));

        when(ubicacionClient.consultarUbicacion("ZONA-001", "PUNTO_ENCUENTRO"))
                .thenReturn(new UbicacionClient.UbicacionExterna(
                        "ZONA-001",
                        "Zona segura",
                        "PUNTO_ENCUENTRO",
                        true
                ));

        AsignacionBrigadista asignacion = new AsignacionBrigadista();
        asignacion.setBrigadistaId(BRIGADISTA_ID);
        asignacion.setEventoId(EVENTO_ID);
        asignacion.setUbicacionId("ZONA-001");
        asignacion.setTipoUbicacion(mock(TipoUbicacion.class));
        asignacion.setEstado(mock(EstadoAsignacion.class));

        when(asignacionRepository.findByEventoIdAndEstado_Nombre(EVENTO_ID, "VIGENTE"))
                .thenReturn(List.of(asignacion));

        when(brigadistaRepository.findByUsuarioIdExterno(BRIGADISTA_ID))
                .thenReturn(Optional.of(new Brigadista(
                        BRIGADISTA_ID,
                        new EstadoBrigadista("ACTIVO", "")
                )));

        when(usuarioClient.consultarUsuario(BRIGADISTA_ID))
                .thenReturn(new UsuarioClient.UsuarioExterno(
                        BRIGADISTA_ID,
                        "Paula Novoa",
                        List.of("BRIGADISTA"),
                        "ACTIVO"
                ));

        when(incidenteMapper.toResponse(any(Incidente.class)))
                .thenReturn(mock(IncidenteResponse.class));

        CrearIncidenteRequest request = new CrearIncidenteRequest(
                null,
                1L,
                "ZONA-001",
                "PUNTO_ENCUENTRO",
                "Necesito ayuda en la zona."
        );

        incidenteService.registrarIncidente(EVENTO_ID, request, USUARIO_ID);

        Incidente incidenteGuardado = capturarIncidenteGuardado();

        assertSame(estadoAsignado, incidenteGuardado.getEstado());
        assertEquals(
                BRIGADISTA_ID,
                incidenteGuardado.getBrigadistaResponsableId()
        );
    }

    @Test
    void brigadistaActivoPuedeRegistrarIncidenteParaOtraPersona() {

        PermisosPrueba.autenticarComo("BRIGADISTA");
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

        PermisosPrueba.autenticarComo("ESTUDIANTE");
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

        PermisosPrueba.autenticarComo("ESTUDIANTE");
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

        PermisosPrueba.autenticarComo("ESTUDIANTE");
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

        PermisosPrueba.autenticarComo("ESTUDIANTE");
        UsuarioClient.UsuarioExterno usuario =
                new UsuarioClient.UsuarioExterno(
                        USUARIO_ID,
                        "Daniel García",
                        List.of("ESTUDIANTE"),
                        "ACTIVO"
                );

        when(usuarioClient.consultarUsuario(USUARIO_ID))
                .thenReturn(usuario);

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

        Brigadista brigadistaActivo = mock(
                Brigadista.class,
                RETURNS_DEEP_STUBS
        );

        when(brigadistaActivo.getEstado().getNombre())
                .thenReturn("ACTIVO");

        when(brigadistaRepository.findByUsuarioIdExterno(BRIGADISTA_ID))
                .thenReturn(Optional.of(brigadistaActivo));

        // Validación del brigadista en MS01.
        when(usuarioClient.consultarUsuario(BRIGADISTA_ID))
                .thenReturn(new UsuarioClient.UsuarioExterno(
                        BRIGADISTA_ID,
                        "Paula Novoa",
                        List.of("BRIGADISTA"),
                        "ACTIVO"
                ));

        when(incidenteMapper.toResponse(any(Incidente.class)))
                .thenReturn(mock(IncidenteResponse.class));

        incidenteService.registrarIncidente(
                EVENTO_ID,
                crearRequestPropio(),
                USUARIO_ID
        );

        Incidente incidenteGuardado = capturarIncidenteGuardado();

        assertNotNull(incidenteGuardado.getId());

        assertSame(
                estadoAsignado,
                incidenteGuardado.getEstado()
        );

        assertEquals(
                BRIGADISTA_ID,
                incidenteGuardado.getBrigadistaResponsableId()
        );

        verify(estadoRepository).findByNombre("ASIGNADO");

        verify(brigadistaRepository)
                .findByUsuarioIdExterno(BRIGADISTA_ID);

        verify(usuarioClient)
                .consultarUsuario(BRIGADISTA_ID);

        verify(historialRepository).save(any(HistorialIncidente.class));
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

    /** Incidente abierto del evento, reportado por el usuario general (la solicitud que MS04 crea desde el check-in). */
    private Incidente incidenteAbiertoPropio(UUID id) {
        Incidente incidente = new Incidente();
        incidente.setId(id);
        incidente.setEventoId(EVENTO_ID);
        incidente.setReportanteId(USUARIO_ID);
        incidente.setEstado(estadoReportado);
        incidente.setPrioridad(prioridad);
        when(estadoReportado.getNombre()).thenReturn("REPORTADO");
        when(incidenteRepository.findById(id)).thenReturn(Optional.of(incidente));
        when(usuarioClient.consultarUsuario(USUARIO_ID)).thenReturn(new UsuarioClient.UsuarioExterno(
                USUARIO_ID, "Daniel García", List.of("ESTUDIANTE"), "ACTIVO"));
        return incidente;
    }

    @Test
    void elPropietarioCompletaLaSolicitudConPrioridadYDescripcion() {
        PermisosPrueba.autenticarComo("ESTUDIANTE");
        UUID id = UUID.randomUUID();
        Incidente incidente = incidenteAbiertoPropio(id);
        PrioridadIncidente baja = mock(PrioridadIncidente.class);
        when(prioridadRepository.findById(3L)).thenReturn(Optional.of(baja));

        incidenteService.editarIncidente(EVENTO_ID, id,
                new EditarIncidenteRequest("Estoy en el piso 3, junto a la escalera.", 3L), USUARIO_ID);

        assertSame(baja, incidente.getPrioridad());
        assertEquals("Estoy en el piso 3, junto a la escalera.", incidente.getDescripcion());
        verify(incidenteRepository).save(incidente);
    }

    @Test
    void sinPrioridadConservaLaQueTeniaElIncidente() {
        PermisosPrueba.autenticarComo("ESTUDIANTE");
        UUID id = UUID.randomUUID();
        Incidente incidente = incidenteAbiertoPropio(id);

        incidenteService.editarIncidente(EVENTO_ID, id, new EditarIncidenteRequest("Solo cambia el texto.", null), USUARIO_ID);

        assertSame(prioridad, incidente.getPrioridad());
        verify(prioridadRepository, never()).findById(3L);
    }

    @Test
    void noPermiteEditarConPrioridadInexistente() {
        PermisosPrueba.autenticarComo("ESTUDIANTE");
        UUID id = UUID.randomUUID();
        incidenteAbiertoPropio(id);
        when(prioridadRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                incidenteService.editarIncidente(EVENTO_ID, id, new EditarIncidenteRequest(null, 99L), USUARIO_ID));

        assertEquals("La prioridad seleccionada no existe.", error.getMessage());
        verify(incidenteRepository, never()).save(any(Incidente.class));
    }
}