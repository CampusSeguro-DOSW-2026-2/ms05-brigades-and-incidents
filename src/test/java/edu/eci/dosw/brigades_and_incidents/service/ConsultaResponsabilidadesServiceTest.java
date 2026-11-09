
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.dto.BusquedaBrigadistasResponse;
import edu.eci.dosw.brigades_and_incidents.dto.BuscarBrigadistaResponse;
import edu.eci.dosw.brigades_and_incidents.entity.AsignacionBrigadista;
import edu.eci.dosw.brigades_and_incidents.entity.EstadoAsignacion;
import edu.eci.dosw.brigades_and_incidents.entity.EstadoIncidente;
import edu.eci.dosw.brigades_and_incidents.entity.Incidente;
import edu.eci.dosw.brigades_and_incidents.entity.PrioridadIncidente;
import edu.eci.dosw.brigades_and_incidents.entity.TipoUbicacion;
import edu.eci.dosw.brigades_and_incidents.repository.AsignacionBrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.IncidenteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultaResponsabilidadesServiceTest {

    @Mock
    private AsignacionBrigadistaRepository asignacionRepository;

    @Mock
    private IncidenteRepository incidenteRepository;

    @Mock
    private BuscarBrigadistaService buscarBrigadistaService;

    @InjectMocks
    private ConsultaResponsabilidadesService service;

    // =========================================================
    // PANEL PERSONAL DEL BRIGADISTA
    // =========================================================

    @Test
    void debeConsultarPanelConAsignacionesVigentes() {

        AsignacionBrigadista asignacion = crearAsignacion(
                "BRIG-001",
                "EVENTO-001",
                "ZONA-001",
                "ZONA_SEGURA",
                "VIGENTE",
                "2026-11-08T10:00:00"
        );

        when(asignacionRepository.findByBrigadistaIdAndEstado_Nombre(
                "BRIG-001", "VIGENTE"
        )).thenReturn(List.of(asignacion));

        when(incidenteRepository.findByEventoIdAndUbicacionId(
                "EVENTO-001", "ZONA-001"
        )).thenReturn(List.of());

        var response = service.consultarPanel("BRIG-001", 0, 10);

        assertNotNull(response);
        assertEquals(1, response.asignaciones().size());
        assertEquals("EVENTO-001", response.asignaciones().get(0).eventoId());
        assertEquals("ZONA-001", response.asignaciones().get(0).ubicacionId());
        assertEquals("ZONA_SEGURA", response.asignaciones().get(0).tipoUbicacion());
        assertTrue(response.tareasPendientes().isEmpty());

        verify(asignacionRepository).findByBrigadistaIdAndEstado_Nombre(
                "BRIG-001", "VIGENTE"
        );
    }

    @Test
    void debeRetornarPanelVacioCuandoBrigadistaNoTieneAsignaciones() {

        when(asignacionRepository.findByBrigadistaIdAndEstado_Nombre(
                "BRIG-002", "VIGENTE"
        )).thenReturn(List.of());

        var response = service.consultarPanel("BRIG-002", 0, 10);

        assertNotNull(response);
        assertTrue(response.asignaciones().isEmpty());
        assertTrue(response.tareasPendientes().isEmpty());
        assertEquals(0, response.metadatosPaginacion().totalElementos());

        verifyNoInteractions(incidenteRepository);
    }

    @Test
    void debeIncluirSoloIncidentesPendientesDelTipoDeUbicacionAsignada() {

        AsignacionBrigadista asignacion = crearAsignacion(
                "BRIG-001",
                "EVENTO-001",
                "ZONA-001",
                "ZONA_SEGURA",
                "VIGENTE",
                "2026-11-08T10:00:00"
        );

        Incidente reportado = crearIncidente(
                "EVENTO-001", "ZONA-001", "ZONA_SEGURA",
                "REPORTADO", "EMERGENCIA",
                "2026-11-08T10:00:00-05:00"
        );

        Incidente asignado = crearIncidente(
                "EVENTO-001", "ZONA-001", "ZONA_SEGURA",
                "ASIGNADO", "URGENCIA_MODERADA",
                "2026-11-08T11:00:00-05:00"
        );

        Incidente resuelto = crearIncidente(
                "EVENTO-001", "ZONA-001", "ZONA_SEGURA",
                "RESUELTO", "POCA_URGENCIA",
                "2026-11-08T12:00:00-05:00"
        );

        Incidente otroTipo = crearIncidente(
                "EVENTO-001", "ZONA-001", "EDIFICIO",
                "REPORTADO", "EMERGENCIA",
                "2026-11-08T14:00:00-05:00"
        );

        when(asignacionRepository.findByBrigadistaIdAndEstado_Nombre(
                "BRIG-001", "VIGENTE"
        )).thenReturn(List.of(asignacion));

        // Este método del repositorio ya filtra por evento y ubicación.
        when(incidenteRepository.findByEventoIdAndUbicacionId(
                "EVENTO-001", "ZONA-001"
        )).thenReturn(List.of(
                reportado, asignado, resuelto, otroTipo
        ));

        var response = service.consultarPanel("BRIG-001", 0, 10);

        assertEquals(2, response.tareasPendientes().size());

        assertTrue(response.tareasPendientes().stream()
                .allMatch(tarea ->
                        tarea.estado().equals("REPORTADO")
                                || tarea.estado().equals("ASIGNADO")
                ));

        assertTrue(response.tareasPendientes().stream()
                .allMatch(tarea ->
                        tarea.tipoUbicacion().equals("ZONA_SEGURA")
                ));

        verify(incidenteRepository).findByEventoIdAndUbicacionId(
                "EVENTO-001", "ZONA-001"
        );
    }

    @Test
    void debeOrdenarIncidentesPorPrioridadYFecha() {

        AsignacionBrigadista asignacion = crearAsignacion(
                "BRIG-001",
                "EVENTO-001",
                "ZONA-001",
                "ZONA_SEGURA",
                "VIGENTE",
                "2026-11-08T10:00:00"
        );

        Incidente pocaUrgencia = crearIncidente(
                "EVENTO-001", "ZONA-001", "ZONA_SEGURA",
                "REPORTADO", "POCA_URGENCIA",
                "2026-11-08T12:00:00-05:00"
        );

        Incidente urgenciaModerada = crearIncidente(
                "EVENTO-001", "ZONA-001", "ZONA_SEGURA",
                "REPORTADO", "URGENCIA_MODERADA",
                "2026-11-08T13:00:00-05:00"
        );

        Incidente emergenciaAntigua = crearIncidente(
                "EVENTO-001", "ZONA-001", "ZONA_SEGURA",
                "REPORTADO", "EMERGENCIA",
                "2026-11-08T10:00:00-05:00"
        );

        Incidente emergenciaReciente = crearIncidente(
                "EVENTO-001", "ZONA-001", "ZONA_SEGURA",
                "ASIGNADO", "EMERGENCIA",
                "2026-11-08T14:00:00-05:00"
        );

        when(asignacionRepository.findByBrigadistaIdAndEstado_Nombre(
                "BRIG-001", "VIGENTE"
        )).thenReturn(List.of(asignacion));

        when(incidenteRepository.findByEventoIdAndUbicacionId(
                "EVENTO-001", "ZONA-001"
        )).thenReturn(List.of(
                pocaUrgencia,
                urgenciaModerada,
                emergenciaAntigua,
                emergenciaReciente
        ));

        var response = service.consultarPanel("BRIG-001", 0, 10);

        assertEquals(4, response.tareasPendientes().size());

        assertEquals(
                "EMERGENCIA",
                response.tareasPendientes().get(0).prioridad()
        );

        assertEquals(
                emergenciaReciente.getId().toString(),
                response.tareasPendientes().get(0).incidenteId()
        );

        assertEquals(
                emergenciaAntigua.getId().toString(),
                response.tareasPendientes().get(1).incidenteId()
        );

        assertEquals(
                "URGENCIA_MODERADA",
                response.tareasPendientes().get(2).prioridad()
        );

        assertEquals(
                "POCA_URGENCIA",
                response.tareasPendientes().get(3).prioridad()
        );
    }

    @Test
    void noDebeDuplicarIncidentesCuandoHayAsignacionesRepetidasDeUbicacion() {

        AsignacionBrigadista primera = crearAsignacion(
                "BRIG-001",
                "EVENTO-001",
                "ZONA-001",
                "ZONA_SEGURA",
                "VIGENTE",
                "2026-11-08T10:00:00"
        );

        AsignacionBrigadista segunda = crearAsignacion(
                "BRIG-001",
                "EVENTO-001",
                "ZONA-001",
                "ZONA_SEGURA",
                "VIGENTE",
                "2026-11-08T11:00:00"
        );

        Incidente incidente = crearIncidente(
                "EVENTO-001", "ZONA-001", "ZONA_SEGURA",
                "REPORTADO", "EMERGENCIA",
                "2026-11-08T10:00:00-05:00"
        );

        when(asignacionRepository.findByBrigadistaIdAndEstado_Nombre(
                "BRIG-001", "VIGENTE"
        )).thenReturn(List.of(primera, segunda));

        when(incidenteRepository.findByEventoIdAndUbicacionId(
                "EVENTO-001", "ZONA-001"
        )).thenReturn(List.of(incidente));

        var response = service.consultarPanel("BRIG-001", 0, 10);

        assertEquals(2, response.asignaciones().size());
        assertEquals(1, response.tareasPendientes().size());

        verify(incidenteRepository, times(2))
                .findByEventoIdAndUbicacionId("EVENTO-001", "ZONA-001");
    }

    @Test
    void debeAplicarPaginacionAlPanel() {

        AsignacionBrigadista asignacion = crearAsignacion(
                "BRIG-001",
                "EVENTO-001",
                "ZONA-001",
                "ZONA_SEGURA",
                "VIGENTE",
                "2026-11-08T10:00:00"
        );

        List<Incidente> incidentes = List.of(
                crearIncidente("EVENTO-001", "ZONA-001", "ZONA_SEGURA",
                        "REPORTADO", "EMERGENCIA", "2026-11-08T10:00:00-05:00"),
                crearIncidente("EVENTO-001", "ZONA-001", "ZONA_SEGURA",
                        "REPORTADO", "URGENCIA_MODERADA", "2026-11-08T11:00:00-05:00"),
                crearIncidente("EVENTO-001", "ZONA-001", "ZONA_SEGURA",
                        "REPORTADO", "POCA_URGENCIA", "2026-11-08T12:00:00-05:00")
        );

        when(asignacionRepository.findByBrigadistaIdAndEstado_Nombre(
                "BRIG-001", "VIGENTE"
        )).thenReturn(List.of(asignacion));

        when(incidenteRepository.findByEventoIdAndUbicacionId(
                "EVENTO-001", "ZONA-001"
        )).thenReturn(incidentes);

        var response = service.consultarPanel("BRIG-001", 1, 2);

        assertEquals(1, response.tareasPendientes().size());
        assertEquals(3, response.metadatosPaginacion().totalElementos());
        assertEquals(2, response.metadatosPaginacion().totalPaginas());
        assertEquals(1, response.metadatosPaginacion().pagina());
    }

    @Test
    void debeRechazarIdentificadorVacioEnPanel() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.consultarPanel(" ", 0, 10)
        );

        verifyNoInteractions(asignacionRepository);
        verifyNoInteractions(incidenteRepository);
    }

    @Test
    void debeRechazarPaginacionInvalidaEnPanel() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.consultarPanel("BRIG-001", -1, 10)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.consultarPanel("BRIG-001", 0, 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.consultarPanel("BRIG-001", 0, 101)
        );

        verifyNoInteractions(asignacionRepository);
        verifyNoInteractions(incidenteRepository);
    }

    // =========================================================
    // LISTADO GENERAL DE RESPONSABILIDADES
    // =========================================================

    @Test
    void debeConsultarListadoGeneralSinFiltros() {

        AsignacionBrigadista asignacion = crearAsignacion(
                "BRIG-001",
                "EVENTO-001",
                "ZONA-001",
                "ZONA_SEGURA",
                "VIGENTE",
                "2026-11-08T10:00:00"
        );

        when(asignacionRepository.findAll())
                .thenReturn(List.of(asignacion));

        var response = service.consultarListadoGeneral(
                null, null, null, null, null, 0, 10
        );

        assertNotNull(response);
        assertEquals(1, response.asignaciones().size());
        assertEquals("BRIG-001", response.asignaciones().get(0).brigadistaId());
        assertEquals("EVENTO-001", response.asignaciones().get(0).eventoId());
        assertEquals(1, response.metadatosPaginacion().totalElementos());

        verify(asignacionRepository).findAll();
    }

    @Test
    void debeFiltrarListadoPorEventoEstadoYUbicacion() {

        AsignacionBrigadista asignacion = crearAsignacion(
                "BRIG-001",
                "EVENTO-001",
                "ZONA-001",
                "ZONA_SEGURA",
                "VIGENTE",
                "2026-11-08T10:00:00"
        );

        // Esta asignación se descarta por evento.
        // Por eso no necesitamos simular los demás atributos.
        AsignacionBrigadista otraAsignacion =
                mock(AsignacionBrigadista.class);

        when(otraAsignacion.getEventoId()).thenReturn("EVENTO-002");

        when(asignacionRepository.findAll())
                .thenReturn(List.of(asignacion, otraAsignacion));

        var response = service.consultarListadoGeneral(
                "EVENTO-001",
                null,
                "vigente",
                "zona_segura",
                "ZONA-001",
                0,
                10
        );

        assertEquals(1, response.asignaciones().size());
        assertEquals("EVENTO-001", response.asignaciones().get(0).eventoId());
        assertEquals("VIGENTE", response.asignaciones().get(0).estado());
        assertEquals("ZONA-001", response.asignaciones().get(0).ubicacionId());
    }

    @Test
    void debeIgnorarEstadoYTipoUbicacionInvalidos() {

        AsignacionBrigadista asignacion = crearAsignacion(
                "BRIG-001",
                "EVENTO-001",
                "ZONA-001",
                "ZONA_SEGURA",
                "VIGENTE",
                "2026-11-08T10:00:00"
        );

        when(asignacionRepository.findAll())
                .thenReturn(List.of(asignacion));

        var response = service.consultarListadoGeneral(
                null,
                null,
                "ESTADO_INEXISTENTE",
                "TIPO_INEXISTENTE",
                null,
                0,
                10
        );

        assertEquals(1, response.asignaciones().size());
    }

    @Test
    void debeFiltrarPorNombreYConservarSoloBrigadistasActivos() {

        AsignacionBrigadista asignacion = crearAsignacion(
                "BRIG-001",
                "EVENTO-001",
                "ZONA-001",
                "ZONA_SEGURA",
                "VIGENTE",
                "2026-11-08T10:00:00"
        );

        BuscarBrigadistaResponse activo = new BuscarBrigadistaResponse(
                "BRIG-001",
                "Ana Torres",
                "BRIGADISTA",
                "ACTIVO",
                List.of()
        );

        BuscarBrigadistaResponse inactivo = new BuscarBrigadistaResponse(
                "BRIG-002",
                "Ana María",
                "BRIGADISTA",
                "INACTIVO",
                List.of()
        );

        BuscarBrigadistaResponse otroRol = new BuscarBrigadistaResponse(
                "USER-003",
                "Ana Pérez",
                "COORDINATOR",
                "ACTIVO",
                List.of()
        );

        when(buscarBrigadistaService.buscarBrigadistas("Ana"))
                .thenReturn(new BusquedaBrigadistasResponse(
                        "Brigadistas encontrados",
                        List.of(activo, inactivo, otroRol)
                ));

        when(asignacionRepository.findAll())
                .thenReturn(List.of(asignacion));

        var response = service.consultarListadoGeneral(
                null,
                "Ana",
                null,
                null,
                null,
                0,
                10
        );

        assertEquals(1, response.asignaciones().size());
        assertEquals("BRIG-001", response.asignaciones().get(0).brigadistaId());

        verify(buscarBrigadistaService).buscarBrigadistas("Ana");
    }

    @Test
    void debeRetornarListadoVacioCuandoNingunBrigadistaCoincide() {

        when(buscarBrigadistaService.buscarBrigadistas("Inexistente"))
                .thenReturn(new BusquedaBrigadistasResponse(
                        "No hay resultados compatibles con la búsqueda",
                        List.of()
                ));

        when(asignacionRepository.findAll())
                .thenReturn(List.of());

        var response = service.consultarListadoGeneral(
                null,
                "Inexistente",
                null,
                null,
                null,
                0,
                10
        );

        assertTrue(response.asignaciones().isEmpty());
        assertEquals(0, response.metadatosPaginacion().totalElementos());
        assertEquals(0, response.metadatosPaginacion().totalPaginas());
    }

    @Test
    void debePropagarErrorCuandoMs01NoEstaDisponible() {

        when(buscarBrigadistaService.buscarBrigadistas("Ana"))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "No fue posible consultar los brigadistas en MS01"
                ));

        assertThrows(
                ResponseStatusException.class,
                () -> service.consultarListadoGeneral(
                        null, "Ana", null, null, null, 0, 10
                )
        );

        verify(asignacionRepository, never()).findAll();
    }

    @Test
    void debeAplicarPaginacionAlListadoGeneral() {

        AsignacionBrigadista primera = crearAsignacion(
                "BRIG-001", "EVENTO-001", "ZONA-001",
                "ZONA_SEGURA", "VIGENTE", "2026-11-08T10:00:00"
        );

        AsignacionBrigadista segunda = crearAsignacion(
                "BRIG-002", "EVENTO-001", "ZONA-002",
                "ZONA_SEGURA", "VIGENTE", "2026-11-08T11:00:00"
        );

        AsignacionBrigadista tercera = crearAsignacion(
                "BRIG-003", "EVENTO-001", "ZONA-003",
                "ZONA_SEGURA", "VIGENTE", "2026-11-08T12:00:00"
        );

        when(asignacionRepository.findAll())
                .thenReturn(List.of(primera, segunda, tercera));

        var response = service.consultarListadoGeneral(
                null, null, null, null, null, 1, 2
        );

        assertEquals(1, response.asignaciones().size());
        assertEquals(3, response.metadatosPaginacion().totalElementos());
        assertEquals(2, response.metadatosPaginacion().totalPaginas());
        assertEquals(1, response.metadatosPaginacion().pagina());
    }

    @Test
    void debeRetornarPaginaVaciaCuandoNoHayMasResultados() {

        AsignacionBrigadista asignacion = crearAsignacion(
                "BRIG-001",
                "EVENTO-001",
                "ZONA-001",
                "ZONA_SEGURA",
                "VIGENTE",
                "2026-11-08T10:00:00"
        );

        when(asignacionRepository.findAll())
                .thenReturn(List.of(asignacion));

        var response = service.consultarListadoGeneral(
                null, null, null, null, null, 5, 10
        );

        assertTrue(response.asignaciones().isEmpty());
        assertEquals(1, response.metadatosPaginacion().totalElementos());
        assertEquals(1, response.metadatosPaginacion().totalPaginas());
    }

    @Test
    void debeIgnorarFiltrosDeTextoVacios() {

        AsignacionBrigadista asignacion = crearAsignacion(
                "BRIG-001",
                "EVENTO-001",
                "ZONA-001",
                "ZONA_SEGURA",
                "VIGENTE",
                "2026-11-08T10:00:00"
        );

        when(asignacionRepository.findAll())
                .thenReturn(List.of(asignacion));

        var response = service.consultarListadoGeneral(
                " ",
                " ",
                " ",
                " ",
                " ",
                0,
                10
        );

        assertEquals(1, response.asignaciones().size());
        verifyNoInteractions(buscarBrigadistaService);
    }

    @Test
    void debeRechazarPaginacionInvalidaEnListadoGeneral() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.consultarListadoGeneral(
                        null, null, null, null, null, -1, 10
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.consultarListadoGeneral(
                        null, null, null, null, null, 0, 101
                )
        );

        verifyNoInteractions(asignacionRepository);
        verifyNoInteractions(buscarBrigadistaService);
    }

    // =========================================================
    // MÉTODOS AUXILIARES
    // =========================================================

    private Incidente crearIncidente(
            String eventoId,
            String ubicacionId,
            String nombreTipoUbicacion,
            String nombreEstado,
            String nombrePrioridad,
            String fecha
    ) {

        Incidente incidente = mock(Incidente.class);

        TipoUbicacion tipo = mock(TipoUbicacion.class);
        EstadoIncidente estado = mock(EstadoIncidente.class);
        PrioridadIncidente prioridad = mock(PrioridadIncidente.class);

        lenient().when(tipo.getNombre()).thenReturn(nombreTipoUbicacion);
        lenient().when(estado.getNombre()).thenReturn(nombreEstado);
        lenient().when(prioridad.getNombre()).thenReturn(nombrePrioridad);

        lenient().when(incidente.getId()).thenReturn(UUID.randomUUID());
        lenient().when(incidente.getEventoId()).thenReturn(eventoId);
        lenient().when(incidente.getUbicacionId()).thenReturn(ubicacionId);
        lenient().when(incidente.getTipoUbicacion()).thenReturn(tipo);
        lenient().when(incidente.getEstado()).thenReturn(estado);
        lenient().when(incidente.getPrioridad()).thenReturn(prioridad);
        lenient().when(incidente.getFechaReporte()).thenReturn(
                OffsetDateTime.parse(fecha)
        );

        return incidente;
    }

    private AsignacionBrigadista crearAsignacion(
            String brigadistaId,
            String eventoId,
            String ubicacionId,
            String nombreTipo,
            String nombreEstado,
            String fechaCreacion
    ) {

        AsignacionBrigadista asignacion =
                mock(AsignacionBrigadista.class);

        TipoUbicacion tipo = mock(TipoUbicacion.class);
        EstadoAsignacion estado = mock(EstadoAsignacion.class);

        lenient().when(tipo.getNombre()).thenReturn(nombreTipo);
        lenient().when(estado.getNombre()).thenReturn(nombreEstado);

        lenient().when(asignacion.getIdAsignacion()).thenReturn(UUID.randomUUID());
        lenient().when(asignacion.getBrigadistaId()).thenReturn(brigadistaId);
        lenient().when(asignacion.getEventoId()).thenReturn(eventoId);
        lenient().when(asignacion.getUbicacionId()).thenReturn(ubicacionId);
        lenient().when(asignacion.getTipoUbicacion()).thenReturn(tipo);
        lenient().when(asignacion.getEstado()).thenReturn(estado);

        lenient().when(asignacion.getFechaCreacion()).thenReturn(
                LocalDateTime.parse(fechaCreacion)
        );

        lenient().when(asignacion.getFechaActualizacion()).thenReturn(
                LocalDateTime.parse(fechaCreacion)
        );

        return asignacion;
    }
}
