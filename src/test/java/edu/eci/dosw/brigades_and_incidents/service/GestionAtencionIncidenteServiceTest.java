
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.client.AlertaClient;
import edu.eci.dosw.brigades_and_incidents.client.UsuarioClient;
import edu.eci.dosw.brigades_and_incidents.dto.AsignarResponsableRequest;
import edu.eci.dosw.brigades_and_incidents.entity.*;
import edu.eci.dosw.brigades_and_incidents.event.IncidenteAuditPublisher;
import edu.eci.dosw.brigades_and_incidents.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestionAtencionIncidenteServiceTest {

    @Mock
    private IncidenteRepository incidenteRepository;

    @Mock
    private EstadoIncidenteRepository estadoIncidenteRepository;

    @Mock
    private BrigadistaRepository brigadistaRepository;

    @Mock
    private AsignacionBrigadistaRepository asignacionRepository;

    @Mock
    private HistorialIncidenteRepository historialRepository;

    @Mock
    private UsuarioClient usuarioClient;

    @Mock
    private IncidenteAuditPublisher auditPublisher;

    @Mock
    private AlertaClient alertaClient;

    private GestionAtencionIncidenteService service;

    private static final UUID INCIDENTE_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String EVENTO_ID = "EVENTO-001";
    private static final String BRIGADISTA_ID = "BRIGADISTA-001";
    private static final String NUEVO_BRIGADISTA_ID = "BRIGADISTA-002";
    private static final String UBICACION_ID = "EDIF-001";
    private static final String COORDINADOR_ID = "COORDINADOR-001";

    @BeforeEach
    void configurar() {
        service = new GestionAtencionIncidenteService(
                incidenteRepository,
                estadoIncidenteRepository,
                brigadistaRepository,
                asignacionRepository,
                historialRepository,
                usuarioClient,
                auditPublisher,
                alertaClient
        );
    }

    // --------------------------------------------------
    // MÉTODOS AUXILIARES
    // --------------------------------------------------

    private Incidente crearIncidente() {
        Incidente incidente = mock(Incidente.class);

        EstadoIncidente estado = mock(EstadoIncidente.class);
        TipoUbicacion tipoUbicacion = mock(TipoUbicacion.class);

        lenient().when(incidente.getId()).thenReturn(INCIDENTE_ID);
        lenient().when(incidente.getEventoId()).thenReturn(EVENTO_ID);
        lenient().when(incidente.getUbicacionId()).thenReturn(UBICACION_ID);
        lenient().when(incidente.getTipoUbicacion()).thenReturn(tipoUbicacion);
        lenient().when(incidente.getEstado()).thenReturn(estado);
        lenient().when(incidente.getBrigadistaResponsableId())
                .thenReturn(BRIGADISTA_ID);

        lenient().when(estado.getNombre()).thenReturn("EN_ATENCION");
        lenient().when(tipoUbicacion.getNombre()).thenReturn("EDIFICIO");

        return incidente;
    }

    private HistorialIncidente crearRegistroHistorial(
            OffsetDateTime fechaCambio
    ) {
        HistorialIncidente historial = mock(HistorialIncidente.class);

        EstadoIncidente estadoAnterior = mock(EstadoIncidente.class);
        EstadoIncidente estadoNuevo = mock(EstadoIncidente.class);

        when(historial.getId()).thenReturn(UUID.randomUUID());
        when(historial.getEstadoAnterior()).thenReturn(estadoAnterior);
        when(historial.getEstadoNuevo()).thenReturn(estadoNuevo);
        when(historial.getResponsableAnteriorId())
                .thenReturn(BRIGADISTA_ID);
        when(historial.getResponsableNuevoId())
                .thenReturn(BRIGADISTA_ID);
        when(historial.getActorId()).thenReturn(BRIGADISTA_ID);
        when(historial.getObservaciones())
                .thenReturn("Atención del incidente");
        when(historial.getFechaCambio()).thenReturn(fechaCambio);

        when(estadoAnterior.getNombre()).thenReturn("ASIGNADO");
        when(estadoNuevo.getNombre()).thenReturn("EN_ATENCION");

        return historial;
    }

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

    private void prepararCoordinador(String actorId, String rol) {
        when(usuarioClient.consultarUsuario(actorId))
                .thenReturn(crearUsuario(actorId, List.of(rol)));
    }

    private void prepararBrigadistaValido(
            String responsableId,
            Incidente incidente
    ) {
        Brigadista brigadista = mock(Brigadista.class);
        EstadoBrigadista estadoBrigadista =
                mock(EstadoBrigadista.class);
        AsignacionBrigadista asignacion =
                mock(AsignacionBrigadista.class);

        // Se obtiene el tipo antes de iniciar el stubbing de Mockito.
        TipoUbicacion tipoUbicacion = incidente.getTipoUbicacion();

        when(usuarioClient.consultarUsuario(responsableId))
                .thenReturn(crearUsuario(
                        responsableId,
                        List.of("BRIGADISTA")
                ));

        when(brigadistaRepository.findByUsuarioIdExterno(responsableId))
                .thenReturn(Optional.of(brigadista));

        when(brigadista.getEstado()).thenReturn(estadoBrigadista);
        when(estadoBrigadista.getNombre()).thenReturn("ACTIVO");

        when(asignacionRepository
                .findByEventoIdAndEstado_Nombre(EVENTO_ID, "VIGENTE"))
                .thenReturn(List.of(asignacion));

        when(asignacion.getBrigadistaId()).thenReturn(responsableId);
        when(asignacion.getUbicacionId()).thenReturn(UBICACION_ID);
        when(asignacion.getTipoUbicacion()).thenReturn(tipoUbicacion);
    }

    private void prepararGuardadoHistorial() {
        when(historialRepository.save(any(HistorialIncidente.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ResponseStatusException ejecutarAsignacionConError(
            String eventoId,
            UUID incidenteId,
            AsignarResponsableRequest request,
            String actorId
    ) {
        return assertThrows(
                ResponseStatusException.class,
                () -> service.asignarResponsable(
                        eventoId,
                        incidenteId,
                        request,
                        actorId
                )
        );
    }

    // --------------------------------------------------
    // CONSULTA DEL HISTORIAL
    // --------------------------------------------------

    @Test
    void auditorPuedeConsultarHistorial() {
        Incidente incidente = crearIncidente();

        OffsetDateTime fechaCambio = OffsetDateTime.parse(
                "2026-11-09T10:30:00-05:00"
        );

        HistorialIncidente historial =
                crearRegistroHistorial(fechaCambio);

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.of(incidente));

        when(usuarioClient.consultarUsuario("AUDITOR-001"))
                .thenReturn(crearUsuario(
                        "AUDITOR-001",
                        List.of("AUDITOR")
                ));

        when(historialRepository
                .findByIncidente_IdOrderByFechaCambioAsc(INCIDENTE_ID))
                .thenReturn(List.of(historial));

        var resultado = service.consultarHistorial(
                EVENTO_ID,
                INCIDENTE_ID,
                "AUDITOR-001"
        );

        assertEquals(1, resultado.size());
        assertEquals("ASIGNADO", resultado.get(0).estadoAnterior());
        assertEquals("EN_ATENCION", resultado.get(0).estadoNuevo());
    }

    @Test
    void historialConservaLaFechaRegistrada() {
        Incidente incidente = crearIncidente();

        OffsetDateTime fechaEsperada = OffsetDateTime.parse(
                "2026-11-09T10:30:00-05:00"
        );

        HistorialIncidente historial =
                crearRegistroHistorial(fechaEsperada);

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.of(incidente));

        when(usuarioClient.consultarUsuario("AUDITOR-001"))
                .thenReturn(crearUsuario(
                        "AUDITOR-001",
                        List.of("AUDITOR")
                ));

        when(historialRepository
                .findByIncidente_IdOrderByFechaCambioAsc(INCIDENTE_ID))
                .thenReturn(List.of(historial));

        var resultado = service.consultarHistorial(
                EVENTO_ID,
                INCIDENTE_ID,
                "AUDITOR-001"
        );

        assertEquals(
                fechaEsperada,
                resultado.get(0).fechaCambio()
        );
    }

    // --------------------------------------------------
    // ACCESO DE BRIGADISTAS AL HISTORIAL
    // --------------------------------------------------

    @Test
    void brigadistaSinAsignacionVigenteNoPuedeConsultarHistorial() {
        Incidente incidente = crearIncidente();

        Brigadista brigadista = mock(Brigadista.class);
        EstadoBrigadista estadoBrigadista =
                mock(EstadoBrigadista.class);

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.of(incidente));

        when(usuarioClient.consultarUsuario(BRIGADISTA_ID))
                .thenReturn(crearUsuario(
                        BRIGADISTA_ID,
                        List.of("BRIGADISTA")
                ));

        when(brigadistaRepository.findByUsuarioIdExterno(BRIGADISTA_ID))
                .thenReturn(Optional.of(brigadista));

        when(brigadista.getEstado()).thenReturn(estadoBrigadista);
        when(estadoBrigadista.getNombre()).thenReturn("ACTIVO");

        when(asignacionRepository
                .findByEventoIdAndEstado_Nombre(EVENTO_ID, "VIGENTE"))
                .thenReturn(List.of());

        assertThrows(
                RuntimeException.class,
                () -> service.consultarHistorial(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        BRIGADISTA_ID
                )
        );

        verifyNoInteractions(historialRepository);
    }

    @Test
    void brigadistaConAsignacionVigentePuedeConsultarHistorial() {
        Incidente incidente = crearIncidente();

        Brigadista brigadista = mock(Brigadista.class);
        EstadoBrigadista estadoBrigadista =
                mock(EstadoBrigadista.class);

        AsignacionBrigadista asignacion =
                mock(AsignacionBrigadista.class);

        TipoUbicacion tipoUbicacion =
                mock(TipoUbicacion.class);

        OffsetDateTime fechaCambio = OffsetDateTime.parse(
                "2026-11-09T10:30:00-05:00"
        );

        HistorialIncidente historial =
                crearRegistroHistorial(fechaCambio);

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.of(incidente));

        when(usuarioClient.consultarUsuario(BRIGADISTA_ID))
                .thenReturn(crearUsuario(
                        BRIGADISTA_ID,
                        List.of("BRIGADISTA")
                ));

        when(brigadistaRepository.findByUsuarioIdExterno(BRIGADISTA_ID))
                .thenReturn(Optional.of(brigadista));

        when(brigadista.getEstado()).thenReturn(estadoBrigadista);
        when(estadoBrigadista.getNombre()).thenReturn("ACTIVO");

        when(asignacionRepository
                .findByEventoIdAndEstado_Nombre(EVENTO_ID, "VIGENTE"))
                .thenReturn(List.of(asignacion));

        when(asignacion.getBrigadistaId()).thenReturn(BRIGADISTA_ID);
        when(asignacion.getUbicacionId()).thenReturn(UBICACION_ID);
        when(asignacion.getTipoUbicacion()).thenReturn(tipoUbicacion);
        when(tipoUbicacion.getNombre()).thenReturn("EDIFICIO");

        when(historialRepository
                .findByIncidente_IdOrderByFechaCambioAsc(INCIDENTE_ID))
                .thenReturn(List.of(historial));

        var resultado = service.consultarHistorial(
                EVENTO_ID,
                INCIDENTE_ID,
                BRIGADISTA_ID
        );

        assertEquals(1, resultado.size());
        assertEquals(fechaCambio, resultado.get(0).fechaCambio());
    }

    // --------------------------------------------------
    // ASIGNACIÓN DE RESPONSABLE
    // --------------------------------------------------

    @Test
    void coordinadorPuedeAsignarResponsable() {
        Incidente incidente = crearIncidente();

        prepararCoordinador(COORDINADOR_ID, "COORDINADOR");
        prepararBrigadistaValido(NUEVO_BRIGADISTA_ID, incidente);
        prepararGuardadoHistorial();

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.of(incidente));

        when(incidenteRepository.save(incidente))
                .thenReturn(incidente);

        var resultado = service.asignarResponsable(
                EVENTO_ID,
                INCIDENTE_ID,
                new AsignarResponsableRequest(
                        NUEVO_BRIGADISTA_ID,
                        "Asignación inicial"
                ),
                COORDINADOR_ID
        );

        assertNotNull(resultado);
        verify(incidente).setBrigadistaResponsableId(NUEVO_BRIGADISTA_ID);
        verify(incidenteRepository).save(incidente);
        verify(historialRepository).save(any(HistorialIncidente.class));
        verify(auditPublisher).publicar(
                eq("ASIGNAR_RESPONSABLE"),
                eq("INCIDENTE"),
                eq(INCIDENTE_ID.toString()),
                eq(EVENTO_ID),
                eq(COORDINADOR_ID),
                eq("EXITOSO"),
                anyString()
        );
    }

    @Test
    void asignarResponsableAvisaAlBrigadistaPorMs03() {
        Incidente incidente = crearIncidente();

        prepararCoordinador(COORDINADOR_ID, "COORDINADOR");
        prepararBrigadistaValido(NUEVO_BRIGADISTA_ID, incidente);
        prepararGuardadoHistorial();

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.of(incidente));
        when(incidenteRepository.save(incidente))
                .thenReturn(incidente);

        service.asignarResponsable(
                EVENTO_ID,
                INCIDENTE_ID,
                new AsignarResponsableRequest(NUEVO_BRIGADISTA_ID, null),
                COORDINADOR_ID
        );

        org.mockito.ArgumentCaptor<AlertaClient.NotificacionAsignacion> aviso =
                org.mockito.ArgumentCaptor.forClass(AlertaClient.NotificacionAsignacion.class);
        verify(alertaClient).notificarAsignacion(aviso.capture());
        assertEquals(NUEVO_BRIGADISTA_ID, aviso.getValue().brigadistaId());
        assertEquals(COORDINADOR_ID, aviso.getValue().actorId());
        assertTrue(aviso.getValue().actorRoles().contains("COORDINADOR"));
    }

    @Test
    void siElAvisoFallaLaAsignacionSeMantiene() {
        Incidente incidente = crearIncidente();

        prepararCoordinador(COORDINADOR_ID, "COORDINADOR");
        prepararBrigadistaValido(NUEVO_BRIGADISTA_ID, incidente);
        prepararGuardadoHistorial();

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.of(incidente));
        when(incidenteRepository.save(incidente))
                .thenReturn(incidente);
        doThrow(new IllegalStateException("MS03 no responde"))
                .when(alertaClient).notificarAsignacion(any());

        var resultado = service.asignarResponsable(
                EVENTO_ID,
                INCIDENTE_ID,
                new AsignarResponsableRequest(NUEVO_BRIGADISTA_ID, null),
                COORDINADOR_ID
        );

        assertNotNull(resultado);
        verify(incidente).setBrigadistaResponsableId(NUEVO_BRIGADISTA_ID);
        verify(incidenteRepository).save(incidente);
    }

    @Test
    void organizadorPuedeAsignarResponsable() {
        Incidente incidente = crearIncidente();

        prepararCoordinador("ORGANIZADOR-001", "ORGANIZADOR");
        prepararBrigadistaValido(NUEVO_BRIGADISTA_ID, incidente);
        prepararGuardadoHistorial();

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.of(incidente));

        when(incidenteRepository.save(incidente))
                .thenReturn(incidente);

        var resultado = service.asignarResponsable(
                EVENTO_ID,
                INCIDENTE_ID,
                new AsignarResponsableRequest(
                        NUEVO_BRIGADISTA_ID,
                        "Asignación por organizador"
                ),
                "ORGANIZADOR-001"
        );

        assertNotNull(resultado);
        verify(incidenteRepository).save(incidente);
    }

    @Test
    void administradorPuedeAsignarResponsable() {
        Incidente incidente = crearIncidente();

        prepararCoordinador("ADMIN-001", "ADMIN");
        prepararBrigadistaValido(NUEVO_BRIGADISTA_ID, incidente);
        prepararGuardadoHistorial();

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.of(incidente));

        when(incidenteRepository.save(incidente))
                .thenReturn(incidente);

        var resultado = service.asignarResponsable(
                EVENTO_ID,
                INCIDENTE_ID,
                new AsignarResponsableRequest(
                        NUEVO_BRIGADISTA_ID,
                        "Asignación por administrador"
                ),
                "ADMIN-001"
        );

        assertNotNull(resultado);
        verify(incidenteRepository).save(incidente);
    }

    @Test
    void noDebeAsignarResponsableSinIdentidad() {
        ResponseStatusException exception =
                ejecutarAsignacionConError(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        new AsignarResponsableRequest(
                                NUEVO_BRIGADISTA_ID,
                                "Observación"
                        ),
                        null
                );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(usuarioClient);
        verifyNoInteractions(incidenteRepository);
    }

    @Test
    void noDebeAsignarResponsableCuandoActorNoEsCoordinador() {
        prepararCoordinador("AUDITOR-001", "AUDITOR");

        ResponseStatusException exception =
                ejecutarAsignacionConError(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        new AsignarResponsableRequest(
                                NUEVO_BRIGADISTA_ID,
                                "Observación"
                        ),
                        "AUDITOR-001"
                );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(incidenteRepository);
    }

    @Test
    void noDebeAsignarResponsableConSolicitudNula() {
        prepararCoordinador(COORDINADOR_ID, "COORDINADOR");

        ResponseStatusException exception =
                ejecutarAsignacionConError(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        null,
                        COORDINADOR_ID
                );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(incidenteRepository);
    }

    @Test
    void noDebeAsignarResponsableConIdentificadorVacio() {
        prepararCoordinador(COORDINADOR_ID, "COORDINADOR");

        ResponseStatusException exception =
                ejecutarAsignacionConError(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        new AsignarResponsableRequest(" ", "Observación"),
                        COORDINADOR_ID
                );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(incidenteRepository);
    }

    @Test
    void noDebeAsignarResponsableSiIncidenteNoExiste() {
        prepararCoordinador(COORDINADOR_ID, "COORDINADOR");

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                ejecutarAsignacionConError(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        new AsignarResponsableRequest(
                                NUEVO_BRIGADISTA_ID,
                                "Observación"
                        ),
                        COORDINADOR_ID
                );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void noDebeAsignarResponsableSiIncidentePerteneceAOtroEvento() {
        Incidente incidente = crearIncidente();

        prepararCoordinador(COORDINADOR_ID, "COORDINADOR");

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.of(incidente));

        ResponseStatusException exception =
                ejecutarAsignacionConError(
                        "EVENTO-OTRO",
                        INCIDENTE_ID,
                        new AsignarResponsableRequest(
                                NUEVO_BRIGADISTA_ID,
                                "Observación"
                        ),
                        COORDINADOR_ID
                );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void noDebeReasignarIncidenteResuelto() {
        Incidente incidente = crearIncidente();
        EstadoIncidente estado = mock(EstadoIncidente.class);

        when(incidente.getEstado()).thenReturn(estado);
        when(estado.getNombre()).thenReturn("RESUELTO");

        prepararCoordinador(COORDINADOR_ID, "COORDINADOR");

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.of(incidente));

        ResponseStatusException exception =
                ejecutarAsignacionConError(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        new AsignarResponsableRequest(
                                NUEVO_BRIGADISTA_ID,
                                "Observación"
                        ),
                        COORDINADOR_ID
                );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(brigadistaRepository);
    }

    @Test
    void noDebeReasignarIncidenteCerrado() {
        Incidente incidente = crearIncidente();
        EstadoIncidente estado = mock(EstadoIncidente.class);

        when(incidente.getEstado()).thenReturn(estado);
        when(estado.getNombre()).thenReturn("CERRADO");

        prepararCoordinador(COORDINADOR_ID, "COORDINADOR");

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.of(incidente));

        ResponseStatusException exception =
                ejecutarAsignacionConError(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        new AsignarResponsableRequest(
                                NUEVO_BRIGADISTA_ID,
                                "Observación"
                        ),
                        COORDINADOR_ID
                );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(brigadistaRepository);
    }

    @Test
    void noDebeAsignarUsuarioQueNoEsBrigadista() {
        Incidente incidente = crearIncidente();

        prepararCoordinador(COORDINADOR_ID, "COORDINADOR");

        when(usuarioClient.consultarUsuario(NUEVO_BRIGADISTA_ID))
                .thenReturn(crearUsuario(
                        NUEVO_BRIGADISTA_ID,
                        List.of("ESTUDIANTE")
                ));

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.of(incidente));

        ResponseStatusException exception =
                ejecutarAsignacionConError(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        new AsignarResponsableRequest(
                                NUEVO_BRIGADISTA_ID,
                                "Observación"
                        ),
                        COORDINADOR_ID
                );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(asignacionRepository);
    }

    @Test
    void noDebeAsignarUsuarioQueNoEstaRegistradoComoBrigadista() {
        Incidente incidente = crearIncidente();

        prepararCoordinador(COORDINADOR_ID, "COORDINADOR");

        when(usuarioClient.consultarUsuario(NUEVO_BRIGADISTA_ID))
                .thenReturn(crearUsuario(
                        NUEVO_BRIGADISTA_ID,
                        List.of("BRIGADISTA")
                ));

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.of(incidente));

        when(brigadistaRepository
                .findByUsuarioIdExterno(NUEVO_BRIGADISTA_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                ejecutarAsignacionConError(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        new AsignarResponsableRequest(
                                NUEVO_BRIGADISTA_ID,
                                "Observación"
                        ),
                        COORDINADOR_ID
                );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void noDebeAsignarBrigadistaInactivo() {
        Incidente incidente = crearIncidente();

        Brigadista brigadista = mock(Brigadista.class);
        EstadoBrigadista estadoBrigadista =
                mock(EstadoBrigadista.class);

        prepararCoordinador(COORDINADOR_ID, "COORDINADOR");

        when(usuarioClient.consultarUsuario(NUEVO_BRIGADISTA_ID))
                .thenReturn(crearUsuario(
                        NUEVO_BRIGADISTA_ID,
                        List.of("BRIGADISTA")
                ));

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.of(incidente));

        when(brigadistaRepository
                .findByUsuarioIdExterno(NUEVO_BRIGADISTA_ID))
                .thenReturn(Optional.of(brigadista));

        when(brigadista.getEstado()).thenReturn(estadoBrigadista);
        when(estadoBrigadista.getNombre()).thenReturn("INACTIVO");

        ResponseStatusException exception =
                ejecutarAsignacionConError(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        new AsignarResponsableRequest(
                                NUEVO_BRIGADISTA_ID,
                                "Observación"
                        ),
                        COORDINADOR_ID
                );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(asignacionRepository);
    }

    @Test
    void noDebeAsignarBrigadistaSinAsignacionVigente() {
        Incidente incidente = crearIncidente();

        Brigadista brigadista = mock(Brigadista.class);
        EstadoBrigadista estadoBrigadista =
                mock(EstadoBrigadista.class);

        prepararCoordinador(COORDINADOR_ID, "COORDINADOR");

        when(usuarioClient.consultarUsuario(NUEVO_BRIGADISTA_ID))
                .thenReturn(crearUsuario(
                        NUEVO_BRIGADISTA_ID,
                        List.of("BRIGADISTA")
                ));

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.of(incidente));

        when(brigadistaRepository
                .findByUsuarioIdExterno(NUEVO_BRIGADISTA_ID))
                .thenReturn(Optional.of(brigadista));

        when(brigadista.getEstado()).thenReturn(estadoBrigadista);
        when(estadoBrigadista.getNombre()).thenReturn("ACTIVO");

        when(asignacionRepository
                .findByEventoIdAndEstado_Nombre(EVENTO_ID, "VIGENTE"))
                .thenReturn(List.of());

        ResponseStatusException exception =
                ejecutarAsignacionConError(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        new AsignarResponsableRequest(
                                NUEVO_BRIGADISTA_ID,
                                "Observación"
                        ),
                        COORDINADOR_ID
                );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void noDebeAsignarAlMismoResponsableActual() {
        Incidente incidente = crearIncidente();

        prepararCoordinador(COORDINADOR_ID, "COORDINADOR");
        prepararBrigadistaValido(BRIGADISTA_ID, incidente);

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.of(incidente));

        ResponseStatusException exception =
                ejecutarAsignacionConError(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        new AsignarResponsableRequest(
                                BRIGADISTA_ID,
                                "Observación"
                        ),
                        COORDINADOR_ID
                );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(incidenteRepository, never()).save(any());
    }
}
