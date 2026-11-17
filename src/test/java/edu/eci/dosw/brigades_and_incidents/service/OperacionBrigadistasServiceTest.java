package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.client.BrigadistaClient;
import edu.eci.dosw.brigades_and_incidents.client.BrigadistaClient.BrigadistaExterno;
import edu.eci.dosw.brigades_and_incidents.dto.AutoasignacionEventoDTO;
import edu.eci.dosw.brigades_and_incidents.dto.AutoasignacionResponse;
import edu.eci.dosw.brigades_and_incidents.dto.BrigadistaGestionDTO;
import edu.eci.dosw.brigades_and_incidents.dto.EventoOperacionDTO;
import edu.eci.dosw.brigades_and_incidents.dto.GestionBrigadistasEventoResponse;
import edu.eci.dosw.brigades_and_incidents.entity.AsignacionBrigadista;
import edu.eci.dosw.brigades_and_incidents.entity.Brigadista;
import edu.eci.dosw.brigades_and_incidents.entity.ConfiguracionOperacionEvento;
import edu.eci.dosw.brigades_and_incidents.entity.EstadoAsignacion;
import edu.eci.dosw.brigades_and_incidents.entity.EstadoBrigadista;
import edu.eci.dosw.brigades_and_incidents.entity.EstadoIncidente;
import edu.eci.dosw.brigades_and_incidents.entity.Incidente;
import edu.eci.dosw.brigades_and_incidents.entity.TipoUbicacion;
import edu.eci.dosw.brigades_and_incidents.entity.ZonaEvento;
import edu.eci.dosw.brigades_and_incidents.event.AsignacionAuditEvent;
import edu.eci.dosw.brigades_and_incidents.exception.ConflictoOperacionException;
import edu.eci.dosw.brigades_and_incidents.exception.Ms01IntegrationException;
import edu.eci.dosw.brigades_and_incidents.exception.RecursoNoEncontradoException;
import edu.eci.dosw.brigades_and_incidents.repository.AsignacionBrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.BrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.ConfiguracionOperacionEventoRepository;
import edu.eci.dosw.brigades_and_incidents.repository.EstadoAsignacionRepository;
import edu.eci.dosw.brigades_and_incidents.repository.EstadoBrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.IncidenteRepository;
import edu.eci.dosw.brigades_and_incidents.repository.TipoUbicacionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OperacionBrigadistasServiceTest {

    private static final String EVENTO = "EVT-1";
    private static final String EDIF_A = "EDIF-A";
    private static final String EDIF_B = "EDIF-B";

    @Mock private ConfiguracionOperacionEventoRepository configuracionRepository;
    @Mock private AsignacionBrigadistaRepository asignacionRepository;
    @Mock private BrigadistaRepository brigadistaRepository;
    @Mock private EstadoBrigadistaRepository estadoBrigadistaRepository;
    @Mock private EstadoAsignacionRepository estadoAsignacionRepository;
    @Mock private TipoUbicacionRepository tipoUbicacionRepository;
    @Mock private IncidenteRepository incidenteRepository;
    @Mock private BrigadistaClient brigadistaClient;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OperacionBrigadistasService service;

    private static final AlcanceGestion COORD = AlcanceGestion.desdeRoles("COORDINADOR");
    private static final AlcanceGestion ORG = AlcanceGestion.desdeRoles("ORGANIZADOR");

    private final EstadoBrigadista activo = new EstadoBrigadista("ACTIVO", "");
    private final EstadoBrigadista inactivo = new EstadoBrigadista("INACTIVO", "");
    private final EstadoAsignacion vigente = new EstadoAsignacion("VIGENTE", "");
    private final EstadoAsignacion retirada = new EstadoAsignacion("RETIRADA", "");
    private final TipoUbicacion edificio = new TipoUbicacion("EDIFICIO", "");

    @BeforeEach
    void setUp() {
        when(estadoBrigadistaRepository.findByNombre("ACTIVO")).thenReturn(Optional.of(activo));
        when(estadoBrigadistaRepository.findByNombre("INACTIVO")).thenReturn(Optional.of(inactivo));
        when(estadoAsignacionRepository.findByNombre("VIGENTE")).thenReturn(Optional.of(vigente));
        when(estadoAsignacionRepository.findByNombre("RETIRADA")).thenReturn(Optional.of(retirada));
        when(tipoUbicacionRepository.findByNombre("EDIFICIO")).thenReturn(Optional.of(edificio));
        when(asignacionRepository.save(any(AsignacionBrigadista.class))).thenAnswer(invocacion -> {
            AsignacionBrigadista asignacion = invocacion.getArgument(0);
            if (asignacion.getIdAsignacion() == null) {
                ReflectionTestUtils.setField(asignacion, "idAsignacion", UUID.randomUUID());
            }
            return asignacion;
        });
        when(brigadistaRepository.save(any(Brigadista.class))).thenAnswer(i -> i.getArgument(0));
        when(asignacionRepository.findByEventoIdAndEstado_Nombre(any(), any())).thenReturn(List.of());
        when(asignacionRepository.findByEventoIdAndBrigadistaIdAndEstado_Nombre(any(), any(), any()))
                .thenReturn(List.of());
    }

    private ConfiguracionOperacionEvento evento(String estado, boolean simulacro) {
        ConfiguracionOperacionEvento configuracion = new ConfiguracionOperacionEvento(EVENTO);
        configuracion.registrarDatosEvento("Evacuación", simulacro, estado, List.of(
                new ZonaEvento(EDIF_B, "Bloque B", null),
                new ZonaEvento(EDIF_A, "Bloque A", "EDIFICIO")
        ));
        when(configuracionRepository.findById(EVENTO)).thenReturn(Optional.of(configuracion));
        return configuracion;
    }

    private Brigadista brigadista(String id, EstadoBrigadista estado) {
        Brigadista brigadista = new Brigadista(id, estado);
        when(brigadistaRepository.findByUsuarioIdExterno(id)).thenReturn(Optional.of(brigadista));
        return brigadista;
    }

    private AsignacionBrigadista asignacion(String brigadistaId, String ubicacionId) {
        AsignacionBrigadista asignacion = new AsignacionBrigadista();
        ReflectionTestUtils.setField(asignacion, "idAsignacion", UUID.randomUUID());
        asignacion.setBrigadistaId(brigadistaId);
        asignacion.setEventoId(EVENTO);
        asignacion.setUbicacionId(ubicacionId);
        asignacion.setTipoUbicacion(edificio);
        asignacion.setEstado(vigente);
        return asignacion;
    }

    private Incidente incidente(String responsable, String estado, boolean eliminado) {
        Incidente incidente = new Incidente();
        incidente.setEventoId(EVENTO);
        incidente.setBrigadistaResponsableId(responsable);
        incidente.setEstado(new EstadoIncidente(estado, ""));
        incidente.setEliminado(eliminado);
        return incidente;
    }

    // ===== Eventos abiertos =====

    @Test
    void consultaEventosAbiertosConEdificiosOrdenadosYConteo() {
        ConfiguracionOperacionEvento configuracion = evento("PROGRAMADO", false);
        when(configuracionRepository.findByEstadoEventoIn(anyCollection())).thenReturn(List.of(configuracion));
        when(asignacionRepository.findByEventoIdAndEstado_Nombre(EVENTO, "VIGENTE"))
                .thenReturn(List.of(asignacion("B1", EDIF_A), asignacion("B2", EDIF_A)));

        List<EventoOperacionDTO> eventos = service.consultarEventosAbiertos(COORD);

        assertEquals(1, eventos.size());
        EventoOperacionDTO dto = eventos.get(0);
        assertEquals("Evacuación", dto.nombre());
        assertTrue(dto.permiteReasignar());
        assertEquals("Bloque A", dto.edificios().get(0).nombre());
        assertEquals(2, dto.edificios().get(0).brigadistasAsignados());
        assertEquals(0, dto.edificios().get(1).brigadistasAsignados());
    }

    // ===== Panel de gestión =====

    @Test
    void panelOrdenaActivosPorEdificioYDespuesInactivos() {
        evento("EN_EJECUCION", false);
        when(brigadistaClient.listarBrigadistas()).thenReturn(List.of(
                new BrigadistaExterno("B1", "Zoe Ruiz", "BRIGADISTA"),
                new BrigadistaExterno("B2", "Ana Díaz", "BRIGADISTA"),
                new BrigadistaExterno("B3", "Luis Pardo", "BRIGADISTA"),
                new BrigadistaExterno("B4", "Carla Mora", "BRIGADISTA"),
                new BrigadistaExterno("X1", "Profesor", "PROFESOR")
        ));
        when(brigadistaRepository.findByUsuarioIdExternoIn(anyCollection())).thenReturn(List.of(
                new Brigadista("B1", activo),
                new Brigadista("B2", activo),
                new Brigadista("B3", inactivo),
                new Brigadista("B4", activo),
                new Brigadista("B5", activo)
        ));
        when(asignacionRepository.findByEventoIdAndEstado_Nombre(EVENTO, "VIGENTE")).thenReturn(List.of(
                asignacion("B1", EDIF_A),
                asignacion("B2", EDIF_B),
                asignacion("B5", EDIF_B)
        ));
        when(incidenteRepository.findByEventoId(EVENTO)).thenReturn(List.of(
                incidente("B1", "EN_ATENCION", false),
                incidente("B1", "RESUELTO", false),
                incidente("B1", "CERRADO", false),
                incidente("B1", "RESUELTO", true),
                incidente(null, "REPORTADO", false)
        ));

        GestionBrigadistasEventoResponse respuesta = service.consultarGestion(EVENTO, COORD);

        List<String> orden = respuesta.brigadistas().stream().map(BrigadistaGestionDTO::brigadistaId).toList();
        assertEquals(List.of("B1", "B2", "B5", "B4", "B3"), orden);

        BrigadistaGestionDTO primero = respuesta.brigadistas().get(0);
        assertEquals("Zoe Ruiz", primero.nombre());
        assertEquals("Bloque A", primero.nombreUbicacion());
        assertEquals(1, primero.incidentesEnAtencion());
        assertEquals(2, primero.incidentesResueltos());
        assertEquals("B5", respuesta.brigadistas().get(2).nombre());
        assertEquals("INACTIVO", respuesta.brigadistas().get(4).estado());
    }

    @Test
    void panelRegistraLocalmenteBrigadistasNuevosDeMs01() {
        evento("PROGRAMADO", false);
        when(brigadistaClient.listarBrigadistas()).thenReturn(List.of(
                new BrigadistaExterno("B9", "Nuevo Brigadista", "BRIGADISTA")
        ));
        when(brigadistaRepository.findByUsuarioIdExternoIn(anyCollection())).thenReturn(List.of());

        GestionBrigadistasEventoResponse respuesta = service.consultarGestion(EVENTO, COORD);

        verify(brigadistaRepository).saveAll(anyList());
        assertEquals("INACTIVO", respuesta.brigadistas().get(0).estado());
    }

    @Test
    void panelResponde503SiMs01NoResponde() {
        evento("PROGRAMADO", false);
        when(brigadistaClient.listarBrigadistas()).thenThrow(new Ms01IntegrationException("caído"));

        assertThrows(ResponseStatusException.class, () -> service.consultarGestion(EVENTO, COORD));
    }

    @Test
    void panelDeEventoInexistenteResponde404() {
        when(configuracionRepository.findById("NO")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.consultarGestion("NO", COORD));
    }

    // ===== Cambio de asignación por el coordinador =====

    @Test
    void coordinadorAsignaBrigadistaSinEdificio() {
        evento("EN_EJECUCION", false);
        brigadista("B1", activo);

        BrigadistaGestionDTO dto = service.cambiarAsignacion(EVENTO, "B1", EDIF_A, "COORD", COORD);

        assertEquals(EDIF_A, dto.ubicacionId());
        assertEquals("Bloque A", dto.nombreUbicacion());
        verify(eventPublisher).publishEvent(any(AsignacionAuditEvent.class));
    }

    @Test
    void coordinadorCambiaEdificioSiNoQuedaVacio() {
        evento("EN_EJECUCION", false);
        brigadista("B1", activo);
        AsignacionBrigadista actual = asignacion("B1", EDIF_A);
        when(asignacionRepository.findByEventoIdAndBrigadistaIdAndEstado_Nombre(EVENTO, "B1", "VIGENTE"))
                .thenReturn(List.of(actual));
        when(asignacionRepository.countByEventoIdAndUbicacionIdAndEstado_NombreAndIdAsignacionNot(
                eq(EVENTO), eq(EDIF_A), eq("VIGENTE"), any())).thenReturn(1L);

        service.cambiarAsignacion(EVENTO, "B1", EDIF_B, "COORD", COORD);

        assertEquals(EDIF_B, actual.getUbicacionId());
    }

    @Test
    void coordinadorNoPuedeDejarEdificioSinBrigadistas() {
        evento("EN_EJECUCION", false);
        brigadista("B1", activo);
        AsignacionBrigadista actual = asignacion("B1", EDIF_A);
        when(asignacionRepository.findByEventoIdAndBrigadistaIdAndEstado_Nombre(EVENTO, "B1", "VIGENTE"))
                .thenReturn(List.of(actual));
        when(asignacionRepository.countByEventoIdAndUbicacionIdAndEstado_NombreAndIdAsignacionNot(
                any(), any(), any(), any())).thenReturn(0L);

        assertThrows(ConflictoOperacionException.class,
                () -> service.cambiarAsignacion(EVENTO, "B1", EDIF_B, "COORD", COORD));
        assertEquals(EDIF_A, actual.getUbicacionId());
    }

    @Test
    void coordinadorNoCambiaAsignacionesEnSimulacros() {
        evento("EN_EJECUCION", true);
        brigadista("B1", activo);

        assertThrows(AccessDeniedException.class,
                () -> service.cambiarAsignacion(EVENTO, "B1", EDIF_A, "COORD", COORD));
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void coordinadorNoVePanelDeSimulacros() {
        evento("PROGRAMADO", true);

        assertThrows(AccessDeniedException.class, () -> service.consultarGestion(EVENTO, COORD));
    }

    @Test
    void organizadorCambiaAsignacionesEnSimulacros() {
        evento("PROGRAMADO", true);
        brigadista("B1", activo);

        BrigadistaGestionDTO dto = service.cambiarAsignacion(EVENTO, "B1", EDIF_A, "ORG", ORG);

        assertEquals(EDIF_A, dto.ubicacionId());
    }

    @Test
    void organizadorNoGestionaEventosReales() {
        evento("EN_EJECUCION", false);
        brigadista("B1", activo);

        assertThrows(AccessDeniedException.class, () -> service.consultarGestion(EVENTO, ORG));
        assertThrows(AccessDeniedException.class,
                () -> service.cambiarAsignacion(EVENTO, "B1", EDIF_A, "ORG", ORG));
    }

    @Test
    void cadaRolVeSoloSusEventos() {
        ConfiguracionOperacionEvento real = evento("PROGRAMADO", false);
        ConfiguracionOperacionEvento simulacro = new ConfiguracionOperacionEvento("SIM-1");
        simulacro.registrarDatosEvento("Simulacro", true, "PROGRAMADO", List.of(new ZonaEvento(EDIF_A, "Bloque A", null)));
        when(configuracionRepository.findByEstadoEventoIn(anyCollection())).thenReturn(List.of(real, simulacro));

        assertEquals(List.of(EVENTO), service.consultarEventosAbiertos(COORD).stream().map(EventoOperacionDTO::eventoId).toList());
        assertEquals(List.of("SIM-1"), service.consultarEventosAbiertos(ORG).stream().map(EventoOperacionDTO::eventoId).toList());
        assertTrue(service.consultarEventosAbiertos(ORG).get(0).permiteReasignar());
        assertEquals(2, service.consultarEventosAbiertos(AlcanceGestion.desdeRoles("ORGANIZADOR, COORDINADOR")).size());
        assertTrue(service.consultarEventosAbiertos(AlcanceGestion.desdeRoles(null)).isEmpty());
        assertTrue(service.consultarEventosAbiertos(AlcanceGestion.desdeRoles("BRIGADISTA")).isEmpty());
    }

    @Test
    void coordinadorNoAsignaBrigadistasInactivos() {
        evento("EN_EJECUCION", false);
        brigadista("B1", inactivo);

        assertThrows(ConflictoOperacionException.class,
                () -> service.cambiarAsignacion(EVENTO, "B1", EDIF_A, "COORD", COORD));
    }

    @Test
    void coordinadorNoAsignaBrigadistaInexistente() {
        evento("EN_EJECUCION", false);
        when(brigadistaRepository.findByUsuarioIdExterno("B1")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.cambiarAsignacion(EVENTO, "B1", EDIF_A, "COORD", COORD));
    }

    @Test
    void coordinadorNoCambiaAsignacionEnEventoFinalizado() {
        evento("FINALIZADO", false);

        assertThrows(ConflictoOperacionException.class,
                () -> service.cambiarAsignacion(EVENTO, "B1", EDIF_A, "COORD", COORD));
    }

    @Test
    void cambioSinActorEsInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> service.cambiarAsignacion(EVENTO, "B1", EDIF_A, " ", COORD));
    }

    // ===== Auto asignación =====

    @Test
    void consultaAutoasignacionRegistraAlBrigadistaNuevoComoInactivo() {
        ConfiguracionOperacionEvento configuracion = evento("PROGRAMADO", true);
        when(brigadistaRepository.findByUsuarioIdExterno("B1")).thenReturn(Optional.empty());
        when(configuracionRepository.findByEstadoEventoIn(anyCollection())).thenReturn(List.of(configuracion));
        when(asignacionRepository.findByEventoIdAndBrigadistaIdAndEstado_Nombre(EVENTO, "B1", "VIGENTE"))
                .thenReturn(List.of(asignacion("B1", EDIF_B)));

        AutoasignacionResponse respuesta = service.consultarAutoasignacion("B1");

        assertEquals("INACTIVO", respuesta.estado());
        assertEquals(1, respuesta.eventos().size());
        assertEquals("Bloque B", respuesta.eventos().get(0).nombreUbicacion());
        assertFalse(respuesta.eventos().get(0).evento().permiteReasignar());
        verify(brigadistaRepository).save(any(Brigadista.class));
    }

    @Test
    void brigadistaActivoSeAutoasigna() {
        evento("PROGRAMADO", true);
        brigadista("B1", activo);

        AutoasignacionEventoDTO dto = service.autoasignarse(EVENTO, "B1", EDIF_A);

        assertEquals(EDIF_A, dto.ubicacionId());
        assertNotNull(dto.idAsignacion());
    }

    @Test
    void brigadistaInactivoNoPuedeAutoasignarse() {
        evento("PROGRAMADO", false);
        brigadista("B1", inactivo);

        assertThrows(ConflictoOperacionException.class, () -> service.autoasignarse(EVENTO, "B1", EDIF_A));
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void noSePuedeAsignarAEdificioFueraDelEvento() {
        evento("PROGRAMADO", false);
        brigadista("B1", activo);

        assertThrows(IllegalArgumentException.class, () -> service.autoasignarse(EVENTO, "B1", "OTRO"));
    }

    @Test
    void noSePuedeAsignarDosVecesAlMismoEdificio() {
        evento("PROGRAMADO", false);
        brigadista("B1", activo);
        when(asignacionRepository.findByEventoIdAndBrigadistaIdAndEstado_Nombre(EVENTO, "B1", "VIGENTE"))
                .thenReturn(List.of(asignacion("B1", EDIF_A)));

        assertThrows(ConflictoOperacionException.class, () -> service.autoasignarse(EVENTO, "B1", EDIF_A));
    }

    @Test
    void alCambiarDeEdificioSeRetiranAsignacionesSobrantes() {
        evento("EN_EJECUCION", false);
        brigadista("B1", activo);
        AsignacionBrigadista primera = asignacion("B1", EDIF_A);
        AsignacionBrigadista sobrante = asignacion("B1", "VIEJO");
        when(asignacionRepository.findByEventoIdAndBrigadistaIdAndEstado_Nombre(EVENTO, "B1", "VIGENTE"))
                .thenReturn(new ArrayList<>(List.of(primera, sobrante)));
        when(asignacionRepository.countByEventoIdAndUbicacionIdAndEstado_NombreAndIdAsignacionNot(
                any(), any(), any(), any())).thenReturn(2L);

        service.autoasignarse(EVENTO, "B1", EDIF_B);

        assertEquals(EDIF_B, primera.getUbicacionId());
        assertEquals("RETIRADA", sobrante.getEstado().getNombre());
    }

    @Test
    void brigadistaRetiraSuAsignacionSiHayOtroEnElEdificio() {
        evento("PROGRAMADO", false);
        AsignacionBrigadista actual = asignacion("B1", EDIF_A);
        when(asignacionRepository.findByEventoIdAndBrigadistaIdAndEstado_Nombre(EVENTO, "B1", "VIGENTE"))
                .thenReturn(List.of(actual));
        when(asignacionRepository.countByEventoIdAndUbicacionIdAndEstado_NombreAndIdAsignacionNot(
                any(), any(), any(), any())).thenReturn(1L);

        AutoasignacionEventoDTO dto = service.retirarAutoasignacion(EVENTO, "B1");

        assertNull(dto.ubicacionId());
        assertEquals("RETIRADA", actual.getEstado().getNombre());
    }

    @Test
    void unicoBrigadistaDelEdificioNoPuedeRetirarse() {
        evento("PROGRAMADO", false);
        when(asignacionRepository.findByEventoIdAndBrigadistaIdAndEstado_Nombre(EVENTO, "B1", "VIGENTE"))
                .thenReturn(List.of(asignacion("B1", EDIF_A)));
        when(asignacionRepository.countByEventoIdAndUbicacionIdAndEstado_NombreAndIdAsignacionNot(
                any(), any(), any(), any())).thenReturn(0L);

        assertThrows(ConflictoOperacionException.class, () -> service.retirarAutoasignacion(EVENTO, "B1"));
    }

    @Test
    void retirarSinAsignacionEsConflicto() {
        evento("PROGRAMADO", false);

        assertThrows(ConflictoOperacionException.class, () -> service.retirarAutoasignacion(EVENTO, "B1"));
    }

    @Test
    void autoasignacionSinBrigadistaEsInvalida() {
        assertThrows(IllegalArgumentException.class, () -> service.autoasignarse(EVENTO, null, EDIF_A));
        assertThrows(IllegalArgumentException.class, () -> service.retirarAutoasignacion(EVENTO, ""));
        assertThrows(IllegalArgumentException.class, () -> service.consultarAutoasignacion(" "));
    }
}
