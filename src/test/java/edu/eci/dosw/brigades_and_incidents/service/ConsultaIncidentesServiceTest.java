
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.client.UsuarioClient;
import edu.eci.dosw.brigades_and_incidents.client.UbicacionClient;
import edu.eci.dosw.brigades_and_incidents.dto.ConsultaIncidentesRequest;
import edu.eci.dosw.brigades_and_incidents.dto.IncidenteResumenDTO;
import edu.eci.dosw.brigades_and_incidents.entity.*;
import edu.eci.dosw.brigades_and_incidents.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConsultaIncidentesServiceTest {

    @Mock
    private IncidenteRepository incidenteRepository;

    @Mock
    private UsuarioClient usuarioClient;

    @Mock
    private BrigadistaRepository brigadistaRepository;

    @Mock
    private AsignacionBrigadistaRepository asignacionRepository;

    @Mock
    private EvidenciaIncidenteRepository evidenciaRepository;

    @Mock
    private UbicacionClient ubicacionClient;

    private ConsultaIncidentesService service;

    private static final UUID INCIDENTE_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String EVENTO_ID = "EVENTO-001";
    private static final String COORDINADOR_ID = "COORDINADOR-001";
    private static final String BRIGADISTA_ID = "BRIGADISTA-001";
    private static final String UBICACION_ID = "EDIF-001";

    @BeforeEach
    void configurar() {
        service = new ConsultaIncidentesService(
                incidenteRepository,
                usuarioClient,
                brigadistaRepository,
                asignacionRepository,
                evidenciaRepository
        );

        service.setUbicacionClient(ubicacionClient);
    }

    // --------------------------------------------------
    // MÉTODOS AUXILIARES
    // --------------------------------------------------

    private UsuarioClient.UsuarioExterno crearUsuario(
            String usuarioId,
            List<String> roles
    ) {
        return new UsuarioClient.UsuarioExterno(
                usuarioId,
                "Usuario de prueba",
                roles,
                "ACTIVO"
        );
    }

    private Incidente crearIncidente(String estadoNombre) {
        return crearIncidente(
                INCIDENTE_ID,
                UBICACION_ID,
                estadoNombre,
                "2026-11-09T10:00:00-05:00"
        );
    }

    private Incidente crearIncidente(
            UUID incidenteId,
            String ubicacionId,
            String estadoNombre,
            String fechaReporte
    ) {
        Incidente incidente = mock(Incidente.class);

        EstadoIncidente estado = mock(EstadoIncidente.class);
        TipoUbicacion tipoUbicacion = mock(TipoUbicacion.class);
        PrioridadIncidente prioridad = mock(PrioridadIncidente.class);

        when(incidente.getId()).thenReturn(incidenteId);
        when(incidente.getEventoId()).thenReturn(EVENTO_ID);
        when(incidente.getNombreAfectado()).thenReturn("Persona afectada");
        when(incidente.getUbicacionId()).thenReturn(ubicacionId);
        when(incidente.getTipoUbicacion()).thenReturn(tipoUbicacion);
        when(incidente.getDescripcion()).thenReturn("Incidente de prueba");
        when(incidente.getPrioridad()).thenReturn(prioridad);
        when(incidente.getEstado()).thenReturn(estado);
        when(incidente.getReportanteId()).thenReturn(BRIGADISTA_ID);
        when(incidente.getBrigadistaResponsableId())
                .thenReturn(BRIGADISTA_ID);

        when(incidente.getFechaReporte()).thenReturn(
                OffsetDateTime.parse(fechaReporte)
        );

        when(incidente.getFechaPrimeraAtencion()).thenReturn(
                OffsetDateTime.parse("2026-11-09T10:10:00-05:00")
        );

        when(incidente.getFechaResolucion()).thenReturn(
                OffsetDateTime.parse("2026-11-09T10:30:00-05:00")
        );

        when(incidente.getObservacionesResolucion())
                .thenReturn("Incidente atendido correctamente");

        when(incidente.getFechaCierre()).thenReturn(
                OffsetDateTime.parse("2026-11-09T10:40:00-05:00")
        );

        when(incidente.isEliminado()).thenReturn(false);

        when(estado.getNombre()).thenReturn(estadoNombre);
        when(tipoUbicacion.getNombre()).thenReturn("EDIFICIO");
        when(prioridad.getNombre()).thenReturn("ALTA");

        return incidente;
    }

    private void configurarCoordinador() {
        when(usuarioClient.consultarUsuario(COORDINADOR_ID))
                .thenReturn(crearUsuario(
                        COORDINADOR_ID,
                        List.of("COORDINADOR")
                ));
    }

    private void configurarBrigadistaSinAsignaciones() {
        Brigadista brigadista = mock(Brigadista.class);
        EstadoBrigadista estado = mock(EstadoBrigadista.class);

        when(usuarioClient.consultarUsuario(BRIGADISTA_ID))
                .thenReturn(crearUsuario(
                        BRIGADISTA_ID,
                        List.of("BRIGADISTA")
                ));

        when(brigadistaRepository.findByUsuarioIdExterno(BRIGADISTA_ID))
                .thenReturn(Optional.of(brigadista));

        when(brigadista.getEstado()).thenReturn(estado);
        when(estado.getNombre()).thenReturn("ACTIVO");

        when(asignacionRepository.findByBrigadistaIdAndEventoId(
                BRIGADISTA_ID,
                EVENTO_ID
        )).thenReturn(List.of());
    }

    private ConsultaIncidentesRequest filtrosVacios() {
        return new ConsultaIncidentesRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private ConsultaIncidentesRequest filtrosOrdenEdificio(
            String direccion,
            Integer pagina,
            Integer tamanio
    ) {
        return new ConsultaIncidentesRequest(
                null,
                null,
                null,
                "edificio",
                direccion,
                pagina,
                tamanio
        );
    }

    private void configurarNombreUbicacion(
            String ubicacionId,
            String nombre
    ) {
        when(ubicacionClient.consultarUbicacion(
                eq(ubicacionId),
                eq("EDIFICIO")
        )).thenReturn(
                new UbicacionClient.UbicacionExterna(
                        ubicacionId,
                        nombre,
                        "EDIFICIO",
                        true
                )
        );
    }

    private String obtenerIdIncidente(Object resultado) {
        return ((IncidenteResumenDTO) resultado).incidenteId();
    }

    // --------------------------------------------------
    // CONSULTA DE INCIDENTES
    // --------------------------------------------------

    @Test
    void coordinadorPuedeConsultarIncidentesDelEvento() {
        configurarCoordinador();

        Incidente incidente = crearIncidente("RESUELTO");

        when(incidenteRepository.findAll(
                ArgumentMatchers.<Specification<Incidente>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(incidente)));

        Page<?> resultado = service.consultarIncidentes(
                EVENTO_ID,
                filtrosVacios(),
                COORDINADOR_ID
        );

        assertEquals(1, resultado.getTotalElements());

        verify(incidenteRepository).findAll(
                ArgumentMatchers.<Specification<Incidente>>any(),
                any(Pageable.class)
        );
    }

    @Test
    void organizadorPuedeConsultarIncidentesDelEvento() {
        when(usuarioClient.consultarUsuario("ORGANIZADOR-001"))
                .thenReturn(crearUsuario("ORGANIZADOR-001", List.of("ORGANIZADOR")));
        Incidente incidente = crearIncidente("RESUELTO");
        when(incidenteRepository.findAll(
                ArgumentMatchers.<Specification<Incidente>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(incidente)));

        Page<?> resultado = service.consultarIncidentes(EVENTO_ID, filtrosVacios(), "ORGANIZADOR-001");

        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void organizadorPuedeConsultarDetalleDelIncidente() {
        when(usuarioClient.consultarUsuario("ORGANIZADOR-001"))
                .thenReturn(crearUsuario("ORGANIZADOR-001", List.of("ORGANIZADOR")));
        Incidente incidente = crearIncidente("RESUELTO");
        when(incidenteRepository.findByIdAndEventoId(INCIDENTE_ID, EVENTO_ID))
                .thenReturn(Optional.of(incidente));
        when(evidenciaRepository.findByIncidenteId(INCIDENTE_ID)).thenReturn(List.of());

        var resultado = service.consultarDetalleIncidente(EVENTO_ID, INCIDENTE_ID, "ORGANIZADOR-001");

        assertEquals(INCIDENTE_ID, resultado.idIncidente());
    }

    @Test
    void coordinadorPuedeConsultarListadoVacio() {
        configurarCoordinador();

        when(incidenteRepository.findAll(
                ArgumentMatchers.<Specification<Incidente>>any(),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        Page<?> resultado = service.consultarIncidentes(
                EVENTO_ID,
                filtrosVacios(),
                COORDINADOR_ID
        );

        assertTrue(resultado.isEmpty());
    }

    @Test
    void consultaAplicaFiltrosYParametrosDePaginacion() {
        configurarCoordinador();

        ConsultaIncidentesRequest filtros =
                new ConsultaIncidentesRequest(
                        "ALTA",
                        "RESUELTO",
                        UBICACION_ID,
                        "prioridad",
                        "DESC",
                        1,
                        20
                );

        when(incidenteRepository.findAll(
                ArgumentMatchers.<Specification<Incidente>>any(),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        service.consultarIncidentes(
                EVENTO_ID,
                filtros,
                COORDINADOR_ID
        );

        org.mockito.ArgumentCaptor<Pageable> captor =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);

        verify(incidenteRepository).findAll(
                ArgumentMatchers.<Specification<Incidente>>any(),
                captor.capture()
        );

        Pageable pageable = captor.getValue();

        assertEquals(1, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());

        assertNotNull(
                pageable.getSort().getOrderFor("prioridad.nombre")
        );

        assertEquals(
                "prioridad.nombre",
                pageable.getSort().getOrderFor("prioridad.nombre").getProperty()
        );

        assertEquals(
                org.springframework.data.domain.Sort.Direction.DESC,
                pageable.getSort().getOrderFor("prioridad.nombre").getDirection()
        );
    }

    @Test
    void consultaSinFiltrosUtilizaValoresPredeterminados() {
        configurarCoordinador();

        when(incidenteRepository.findAll(
                ArgumentMatchers.<Specification<Incidente>>any(),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        service.consultarIncidentes(
                EVENTO_ID,
                null,
                COORDINADOR_ID
        );

        org.mockito.ArgumentCaptor<Pageable> captor =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);

        verify(incidenteRepository).findAll(
                ArgumentMatchers.<Specification<Incidente>>any(),
                captor.capture()
        );

        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
    }

    @Test
    void consultaLimitaElTamanioMaximoDePagina() {
        configurarCoordinador();

        ConsultaIncidentesRequest filtros =
                new ConsultaIncidentesRequest(
                        null, null, null, null, null, 0, 500
                );

        when(incidenteRepository.findAll(
                ArgumentMatchers.<Specification<Incidente>>any(),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        service.consultarIncidentes(
                EVENTO_ID,
                filtros,
                COORDINADOR_ID
        );

        org.mockito.ArgumentCaptor<Pageable> captor =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);

        verify(incidenteRepository).findAll(
                ArgumentMatchers.<Specification<Incidente>>any(),
                captor.capture()
        );

        assertEquals(100, captor.getValue().getPageSize());
    }

    @Test
    void brigadistaActivoPuedeConsultarIncidentesVisibles() {
        configurarBrigadistaSinAsignaciones();

        Incidente incidente = crearIncidente("REPORTADO");

        when(incidenteRepository.findAll(
                ArgumentMatchers.<Specification<Incidente>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(incidente)));

        Page<?> resultado = service.consultarIncidentes(
                EVENTO_ID,
                filtrosVacios(),
                BRIGADISTA_ID
        );

        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void estudianteConsultaSoloSusPropiasSolicitudes() {
        when(usuarioClient.consultarUsuario("ESTUDIANTE-001"))
                .thenReturn(crearUsuario("ESTUDIANTE-001", List.of("ESTUDIANTE")));

        Incidente incidente = crearIncidente("REPORTADO");

        when(incidenteRepository.findAll(
                ArgumentMatchers.<Specification<Incidente>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(incidente)));

        Page<?> resultado = service.consultarIncidentes(
                EVENTO_ID,
                filtrosVacios(),
                "ESTUDIANTE-001"
        );

        // El filtro "reportados por el usuario" lo aplica la especificación en la base de datos
        assertEquals(1, resultado.getTotalElements());
        verify(incidenteRepository).findAll(
                ArgumentMatchers.<Specification<Incidente>>any(),
                any(Pageable.class)
        );
    }

    @Test
    void estudiantePuedeConsultarDetalleDeSuSolicitud() {
        // crearIncidente deja como reportante a BRIGADISTA_ID; aquí ese usuario es un estudiante
        when(usuarioClient.consultarUsuario(BRIGADISTA_ID))
                .thenReturn(crearUsuario(BRIGADISTA_ID, List.of("ESTUDIANTE")));

        Incidente incidente = crearIncidente("REPORTADO");

        when(incidenteRepository.findByIdAndEventoId(INCIDENTE_ID, EVENTO_ID))
                .thenReturn(Optional.of(incidente));
        when(evidenciaRepository.findByIncidenteId(INCIDENTE_ID))
                .thenReturn(List.of());

        var resultado = service.consultarDetalleIncidente(
                EVENTO_ID,
                INCIDENTE_ID,
                BRIGADISTA_ID
        );

        assertEquals(INCIDENTE_ID, resultado.idIncidente());
    }

    @Test
    void estudianteNoPuedeConsultarSolicitudAjena() {
        when(usuarioClient.consultarUsuario("ESTUDIANTE-001"))
                .thenReturn(crearUsuario("ESTUDIANTE-001", List.of("ESTUDIANTE")));

        Incidente incidente = crearIncidente("REPORTADO");

        when(incidenteRepository.findByIdAndEventoId(INCIDENTE_ID, EVENTO_ID))
                .thenReturn(Optional.of(incidente));

        assertThrows(
                ResponseStatusException.class,
                () -> service.consultarDetalleIncidente(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        "ESTUDIANTE-001"
                )
        );

        verifyNoInteractions(evidenciaRepository);
    }

    @Test
    void auditorPuedeConsultarListadoDeIncidentes() {
        when(usuarioClient.consultarUsuario("AUDITOR-001"))
                .thenReturn(crearUsuario("AUDITOR-001", List.of("AUDITOR")));
        Incidente incidente = crearIncidente("RESUELTO");
        when(incidenteRepository.findAll(
                ArgumentMatchers.<Specification<Incidente>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(incidente)));

        Page<?> resultado = service.consultarIncidentes(EVENTO_ID, filtrosVacios(), "AUDITOR-001");

        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void auditorPuedeConsultarDetalleDelIncidente() {
        when(usuarioClient.consultarUsuario("AUDITOR-001"))
                .thenReturn(crearUsuario("AUDITOR-001", List.of("AUDITOR")));
        Incidente incidente = crearIncidente("RESUELTO");
        when(incidenteRepository.findByIdAndEventoId(INCIDENTE_ID, EVENTO_ID))
                .thenReturn(Optional.of(incidente));
        when(evidenciaRepository.findByIncidenteId(INCIDENTE_ID)).thenReturn(List.of());

        var resultado = service.consultarDetalleIncidente(EVENTO_ID, INCIDENTE_ID, "AUDITOR-001");

        assertEquals(INCIDENTE_ID, resultado.idIncidente());
    }

    @Test
    void consultaSinIdentidadEsRechazada() {
        assertThrows(
                ResponseStatusException.class,
                () -> service.consultarIncidentes(
                        EVENTO_ID,
                        filtrosVacios(),
                        null
                )
        );

        verifyNoInteractions(usuarioClient);
    }

    @Test
    void consultaSinEventoEsRechazada() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.consultarIncidentes(
                        " ",
                        filtrosVacios(),
                        COORDINADOR_ID
                )
        );

        verifyNoInteractions(incidenteRepository);
    }

    @Test
    void usuarioInactivoNoPuedeConsultar() {
        when(usuarioClient.consultarUsuario("USUARIO-001"))
                .thenReturn(new UsuarioClient.UsuarioExterno(
                        "USUARIO-001",
                        "Usuario inactivo",
                        List.of("COORDINADOR"),
                        "INACTIVO"
                ));

        assertThrows(
                ResponseStatusException.class,
                () -> service.consultarIncidentes(
                        EVENTO_ID,
                        filtrosVacios(),
                        "USUARIO-001"
                )
        );

        verifyNoInteractions(incidenteRepository);
    }

    // --------------------------------------------------
    // ORDENAMIENTO POR EDIFICIO
    // --------------------------------------------------

    @Test
    void consultaOrdenaIncidentesPorEdificioAscendente() {
        configurarCoordinador();

        UUID idA = UUID.fromString(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        );
        UUID idB = UUID.fromString(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
        );
        UUID idC = UUID.fromString(
                "cccccccc-cccc-cccc-cccc-cccccccccccc"
        );

        Incidente incidenteEdificioC = crearIncidente(
                idC, "EDIF-003", "REPORTADO",
                "2026-11-09T10:00:00-05:00"
        );

        Incidente incidenteEdificioA = crearIncidente(
                idA, "EDIF-001", "REPORTADO",
                "2026-11-09T11:00:00-05:00"
        );

        Incidente incidenteEdificioB = crearIncidente(
                idB, "EDIF-002", "REPORTADO",
                "2026-11-09T12:00:00-05:00"
        );

        configurarNombreUbicacion("EDIF-001", "Edificio A");
        configurarNombreUbicacion("EDIF-002", "Edificio B");
        configurarNombreUbicacion("EDIF-003", "Edificio C");

        when(incidenteRepository.findAll(
                ArgumentMatchers.<Specification<Incidente>>any()
        )).thenReturn(List.of(
                incidenteEdificioC,
                incidenteEdificioA,
                incidenteEdificioB
        ));

        Page<?> resultado = service.consultarIncidentes(
                EVENTO_ID,
                filtrosOrdenEdificio("ASC", 0, 10),
                COORDINADOR_ID
        );

        assertEquals(3, resultado.getTotalElements());
        assertEquals(3, resultado.getContent().size());

        assertEquals(idA.toString(),
                obtenerIdIncidente(resultado.getContent().get(0)));

        assertEquals(idB.toString(),
                obtenerIdIncidente(resultado.getContent().get(1)));

        assertEquals(idC.toString(),
                obtenerIdIncidente(resultado.getContent().get(2)));
    }

    @Test
    void consultaOrdenaIncidentesPorEdificioDescendente() {
        configurarCoordinador();

        UUID idA = UUID.fromString(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        );
        UUID idB = UUID.fromString(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
        );
        UUID idC = UUID.fromString(
                "cccccccc-cccc-cccc-cccc-cccccccccccc"
        );

        Incidente incidenteEdificioA = crearIncidente(
                idA, "EDIF-001", "REPORTADO",
                "2026-11-09T10:00:00-05:00"
        );

        Incidente incidenteEdificioB = crearIncidente(
                idB, "EDIF-002", "REPORTADO",
                "2026-11-09T11:00:00-05:00"
        );

        Incidente incidenteEdificioC = crearIncidente(
                idC, "EDIF-003", "REPORTADO",
                "2026-11-09T12:00:00-05:00"
        );

        configurarNombreUbicacion("EDIF-001", "Edificio A");
        configurarNombreUbicacion("EDIF-002", "Edificio B");
        configurarNombreUbicacion("EDIF-003", "Edificio C");

        when(incidenteRepository.findAll(
                ArgumentMatchers.<Specification<Incidente>>any()
        )).thenReturn(List.of(
                incidenteEdificioA,
                incidenteEdificioB,
                incidenteEdificioC
        ));

        Page<?> resultado = service.consultarIncidentes(
                EVENTO_ID,
                filtrosOrdenEdificio("DESC", 0, 10),
                COORDINADOR_ID
        );

        assertEquals(3, resultado.getTotalElements());

        assertEquals(idC.toString(),
                obtenerIdIncidente(resultado.getContent().get(0)));

        assertEquals(idB.toString(),
                obtenerIdIncidente(resultado.getContent().get(1)));

        assertEquals(idA.toString(),
                obtenerIdIncidente(resultado.getContent().get(2)));
    }

    @Test
    void consultaAplicaPaginacionDespuesDeOrdenarPorEdificio() {
        configurarCoordinador();

        UUID idA = UUID.fromString(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        );
        UUID idB = UUID.fromString(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
        );
        UUID idC = UUID.fromString(
                "cccccccc-cccc-cccc-cccc-cccccccccccc"
        );

        Incidente incidenteEdificioC = crearIncidente(
                idC, "EDIF-003", "REPORTADO",
                "2026-11-09T10:00:00-05:00"
        );

        Incidente incidenteEdificioA = crearIncidente(
                idA, "EDIF-001", "REPORTADO",
                "2026-11-09T11:00:00-05:00"
        );

        Incidente incidenteEdificioB = crearIncidente(
                idB, "EDIF-002", "REPORTADO",
                "2026-11-09T12:00:00-05:00"
        );

        configurarNombreUbicacion("EDIF-001", "Edificio A");
        configurarNombreUbicacion("EDIF-002", "Edificio B");
        configurarNombreUbicacion("EDIF-003", "Edificio C");

        when(incidenteRepository.findAll(
                ArgumentMatchers.<Specification<Incidente>>any()
        )).thenReturn(List.of(
                incidenteEdificioC,
                incidenteEdificioA,
                incidenteEdificioB
        ));

        Page<?> resultado = service.consultarIncidentes(
                EVENTO_ID,
                filtrosOrdenEdificio("ASC", 1, 1),
                COORDINADOR_ID
        );

        assertEquals(3, resultado.getTotalElements());
        assertEquals(3, resultado.getTotalPages());
        assertEquals(1, resultado.getNumber());
        assertEquals(1, resultado.getSize());
        assertEquals(1, resultado.getContent().size());

        assertEquals(
                idB.toString(),
                obtenerIdIncidente(resultado.getContent().get(0))
        );

        verify(incidenteRepository, never()).findAll(
                ArgumentMatchers.<Specification<Incidente>>any(),
                any(Pageable.class)
        );
    }

    @Test
    void consultaPorEdificioContinuaSiMs02NoEncuentraUbicacion() {
        configurarCoordinador();

        Incidente incidente = crearIncidente(
                INCIDENTE_ID,
                "EDIF-INEXISTENTE",
                "REPORTADO",
                "2026-11-09T10:00:00-05:00"
        );

        when(incidenteRepository.findAll(
                ArgumentMatchers.<Specification<Incidente>>any()
        )).thenReturn(List.of(incidente));

        when(ubicacionClient.consultarUbicacion(
                "EDIF-INEXISTENTE",
                "EDIFICIO"
        )).thenReturn(null);

        Page<?> resultado = assertDoesNotThrow(
                () -> service.consultarIncidentes(
                        EVENTO_ID,
                        filtrosOrdenEdificio("ASC", 0, 10),
                        COORDINADOR_ID
                )
        );

        assertEquals(1, resultado.getTotalElements());
        assertEquals(1, resultado.getContent().size());

        verify(ubicacionClient).consultarUbicacion(
                "EDIF-INEXISTENTE",
                "EDIFICIO"
        );
    }

    // --------------------------------------------------
    // CONSULTA DEL DETALLE
    // --------------------------------------------------

    @Test
    void coordinadorPuedeConsultarDetalleDelIncidente() {
        configurarCoordinador();

        Incidente incidente = crearIncidente("RESUELTO");

        when(incidenteRepository.findByIdAndEventoId(
                INCIDENTE_ID,
                EVENTO_ID
        )).thenReturn(Optional.of(incidente));

        when(evidenciaRepository.findByIncidenteId(INCIDENTE_ID))
                .thenReturn(List.of());

        var resultado = service.consultarDetalleIncidente(
                EVENTO_ID,
                INCIDENTE_ID,
                COORDINADOR_ID
        );

        assertEquals(INCIDENTE_ID, resultado.idIncidente());
        assertEquals(EVENTO_ID, resultado.eventoId());
        assertEquals("ALTA", resultado.prioridad());
        assertEquals("RESUELTO", resultado.estado());
        assertTrue(resultado.evidencias().isEmpty());
    }

    @Test
    void detalleIncluyeLasEvidenciasDelIncidente() {
        configurarCoordinador();

        Incidente incidente = crearIncidente("RESUELTO");

        EvidenciaIncidente evidencia = mock(EvidenciaIncidente.class);

        when(evidencia.getId()).thenReturn(UUID.randomUUID());
        when(evidencia.getNombreArchivo()).thenReturn("foto.jpg");
        when(evidencia.getTipoContenido()).thenReturn("image/jpeg");
        when(evidencia.getReferenciaArchivo()).thenReturn("archivo-001");
        when(evidencia.getFechaCarga()).thenReturn(
                OffsetDateTime.parse("2026-11-09T10:15:00-05:00")
        );

        when(incidenteRepository.findByIdAndEventoId(
                INCIDENTE_ID,
                EVENTO_ID
        )).thenReturn(Optional.of(incidente));

        when(evidenciaRepository.findByIncidenteId(INCIDENTE_ID))
                .thenReturn(List.of(evidencia));

        var resultado = service.consultarDetalleIncidente(
                EVENTO_ID,
                INCIDENTE_ID,
                COORDINADOR_ID
        );

        assertEquals(1, resultado.evidencias().size());
        assertEquals(
                "foto.jpg",
                resultado.evidencias().get(0).nombreArchivo()
        );
    }

    @Test
    void detalleDeIncidenteInexistenteDevuelveError() {
        configurarCoordinador();

        when(incidenteRepository.findByIdAndEventoId(
                INCIDENTE_ID,
                EVENTO_ID
        )).thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> service.consultarDetalleIncidente(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        COORDINADOR_ID
                )
        );
    }

    @Test
    void brigadistaPuedeConsultarIncidenteQueReporto() {
        configurarBrigadistaSinAsignaciones();

        Incidente incidente = crearIncidente("REPORTADO");

        when(incidenteRepository.findByIdAndEventoId(
                INCIDENTE_ID,
                EVENTO_ID
        )).thenReturn(Optional.of(incidente));

        when(evidenciaRepository.findByIncidenteId(INCIDENTE_ID))
                .thenReturn(List.of());

        var resultado = service.consultarDetalleIncidente(
                EVENTO_ID,
                INCIDENTE_ID,
                BRIGADISTA_ID
        );

        assertEquals(INCIDENTE_ID, resultado.idIncidente());
    }

    @Test
    void brigadistaNoPuedeConsultarIncidenteAjeno() {
        configurarBrigadistaSinAsignaciones();

        Incidente incidente = crearIncidente("REPORTADO");

        when(incidente.getReportanteId()).thenReturn("OTRO-USUARIO");
        when(incidente.getBrigadistaResponsableId())
                .thenReturn("OTRO-BRIGADISTA");

        when(incidenteRepository.findByIdAndEventoId(
                INCIDENTE_ID,
                EVENTO_ID
        )).thenReturn(Optional.of(incidente));

        assertThrows(
                ResponseStatusException.class,
                () -> service.consultarDetalleIncidente(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        BRIGADISTA_ID
                )
        );

        verifyNoInteractions(evidenciaRepository);
    }

    @Test
    void detalleDeIncidenteEliminadoNoEstaDisponible() {
        configurarCoordinador();

        Incidente incidente = crearIncidente("RESUELTO");

        when(incidente.isEliminado()).thenReturn(true);

        when(incidenteRepository.findByIdAndEventoId(
                INCIDENTE_ID,
                EVENTO_ID
        )).thenReturn(Optional.of(incidente));

        assertThrows(
                ResponseStatusException.class,
                () -> service.consultarDetalleIncidente(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        COORDINADOR_ID
                )
        );

        verifyNoInteractions(evidenciaRepository);
    }

    // --------------------------------------------------
    // ELIMINACIÓN LÓGICA
    // --------------------------------------------------

    @Test
    void coordinadorPuedeEliminarIncidenteResuelto() {
        configurarCoordinador();

        Incidente incidente = crearIncidente("RESUELTO");

        when(incidenteRepository.findByIdAndEventoIdAndEliminadoFalse(
                INCIDENTE_ID,
                EVENTO_ID
        )).thenReturn(Optional.of(incidente));

        service.eliminarIncidente(
                EVENTO_ID,
                INCIDENTE_ID,
                COORDINADOR_ID
        );

        verify(incidente).setEliminado(true);
        verify(incidenteRepository).save(incidente);
    }

    @Test
    void coordinadorNoPuedeEliminarIncidenteReportado() {
        verificarEstadoNoEliminable("REPORTADO");
    }

    @Test
    void coordinadorNoPuedeEliminarIncidenteEnAtencion() {
        verificarEstadoNoEliminable("EN_ATENCION");
    }

    @Test
    void coordinadorNoPuedeEliminarIncidenteAsignado() {
        verificarEstadoNoEliminable("ASIGNADO");
    }

    @Test
    void coordinadorNoPuedeEliminarIncidenteCerrado() {
        verificarEstadoNoEliminable("CERRADO");
    }

    private void verificarEstadoNoEliminable(String estado) {
        configurarCoordinador();

        Incidente incidente = crearIncidente(estado);

        when(incidenteRepository.findByIdAndEventoIdAndEliminadoFalse(
                INCIDENTE_ID,
                EVENTO_ID
        )).thenReturn(Optional.of(incidente));

        assertThrows(
                ResponseStatusException.class,
                () -> service.eliminarIncidente(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        COORDINADOR_ID
                )
        );

        verify(incidente, never()).setEliminado(true);
        verify(incidenteRepository, never()).save(any(Incidente.class));
    }

    @Test
    void brigadistaNoPuedeEliminarIncidente() {
        when(usuarioClient.consultarUsuario(BRIGADISTA_ID))
                .thenReturn(crearUsuario(
                        BRIGADISTA_ID,
                        List.of("BRIGADISTA")
                ));

        assertThrows(
                ResponseStatusException.class,
                () -> service.eliminarIncidente(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        BRIGADISTA_ID
                )
        );

        verify(incidenteRepository, never()).save(any(Incidente.class));
    }

    @Test
    void auditorNoPuedeEliminarIncidente() {
        when(usuarioClient.consultarUsuario("AUDITOR-001"))
                .thenReturn(crearUsuario(
                        "AUDITOR-001",
                        List.of("AUDITOR")
                ));

        assertThrows(
                ResponseStatusException.class,
                () -> service.eliminarIncidente(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        "AUDITOR-001"
                )
        );

        verify(incidenteRepository, never()).save(any(Incidente.class));
    }

    @Test
    void incidenteInexistenteNoPuedeEliminarse() {
        configurarCoordinador();

        when(incidenteRepository.findByIdAndEventoIdAndEliminadoFalse(
                INCIDENTE_ID,
                EVENTO_ID
        )).thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> service.eliminarIncidente(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        COORDINADOR_ID
                )
        );

        verify(incidenteRepository, never()).save(any(Incidente.class));
    }

    @Test
    void incidenteYaEliminadoNoPuedeEliminarseNuevamente() {
        configurarCoordinador();

        when(incidenteRepository.findByIdAndEventoIdAndEliminadoFalse(
                INCIDENTE_ID,
                EVENTO_ID
        )).thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> service.eliminarIncidente(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        COORDINADOR_ID
                )
        );

        verify(incidenteRepository, never()).save(any(Incidente.class));
    }

    @Test
    void eliminacionSinIdentidadEsRechazada() {
        assertThrows(
                ResponseStatusException.class,
                () -> service.eliminarIncidente(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        null
                )
        );

        verifyNoInteractions(usuarioClient);
        verifyNoInteractions(incidenteRepository);
    }

    @Test
    void eliminacionSinEventoEsRechazada() {
        assertThrows(
                ResponseStatusException.class,
                () -> service.eliminarIncidente(
                        " ",
                        INCIDENTE_ID,
                        COORDINADOR_ID
                )
        );

        verifyNoInteractions(usuarioClient);
    }

    @Test
    void eliminacionSinIdentificadorDeIncidenteEsRechazada() {
        assertThrows(
                ResponseStatusException.class,
                () -> service.eliminarIncidente(
                        EVENTO_ID,
                        null,
                        COORDINADOR_ID
                )
        );

        verifyNoInteractions(usuarioClient);
    }
}
