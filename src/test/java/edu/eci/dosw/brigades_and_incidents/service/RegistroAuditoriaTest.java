
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.entity.RegistroAuditoria;
import edu.eci.dosw.brigades_and_incidents.entity.TipoRegistroAuditoria;
import edu.eci.dosw.brigades_and_incidents.event.AsignacionAuditEvent;
import edu.eci.dosw.brigades_and_incidents.event.IncidenteAuditEvent;
import edu.eci.dosw.brigades_and_incidents.event.RegistroAuditoriaEventListener;
import edu.eci.dosw.brigades_and_incidents.exception.RecursoNoEncontradoException;
import edu.eci.dosw.brigades_and_incidents.repository.RegistroAuditoriaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.TipoRegistroAuditoriaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistroAuditoriaTest {

    @Mock
    private RegistroAuditoriaRepository registroRepository;

    @Mock
    private TipoRegistroAuditoriaRepository tipoRepository;

    @Mock
    private RegistroAuditoriaService auditoriaServiceMock;

    private RegistroAuditoriaService auditoriaService;

    private RegistroAuditoriaEventListener listener;

    private TipoRegistroAuditoria tipoIncidente;

    @BeforeEach
    void configurar() {
        auditoriaService = new RegistroAuditoriaService(
                registroRepository,
                tipoRepository
        );

        listener = new RegistroAuditoriaEventListener(
                auditoriaServiceMock
        );

        tipoIncidente = new TipoRegistroAuditoria();
        tipoIncidente.setId(3L);
        tipoIncidente.setNombre("INCIDENTE");
    }

    // =====================================================
    // 1. REGISTRO DE ACCIONES
    // =====================================================

    @Test
    void debeRegistrarUnaAccionCorrectamente() {

        when(tipoRepository.findByNombre("INCIDENTE"))
                .thenReturn(Optional.of(tipoIncidente));

        when(registroRepository.save(any(RegistroAuditoria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegistroAuditoria resultado = auditoriaService.registrarAccion(
                "incidente",
                "usuario-123",
                "CREAR_INCIDENTE",
                "INCIDENTE",
                "incidente-456",
                "evento-789",
                "MS05",
                "EXITOSO",
                "Creacion de incidente"
        );

        ArgumentCaptor<RegistroAuditoria> captor =
                ArgumentCaptor.forClass(RegistroAuditoria.class);

        verify(registroRepository).save(captor.capture());

        RegistroAuditoria guardado = captor.getValue();

        assertNotNull(resultado);
        assertEquals("INCIDENTE", guardado.getTipoRegistro().getNombre());
        assertEquals("usuario-123", guardado.getUsuarioOrigen());
        assertEquals("CREAR_INCIDENTE", guardado.getAccion());
        assertEquals("INCIDENTE", guardado.getRecursoTipo());
        assertEquals("incidente-456", guardado.getRecursoId());
        assertEquals("evento-789", guardado.getEventoId());
        assertEquals("MS05", guardado.getOrigen());
        assertEquals("EXITOSO", guardado.getResultado());
        assertEquals("Creacion de incidente", guardado.getDetalle());
        assertNotNull(guardado.getFechaHoraRegistro());
    }

    @Test
    void debeNormalizarLosCamposAntesDeRegistrar() {

        when(tipoRepository.findByNombre("INCIDENTE"))
                .thenReturn(Optional.of(tipoIncidente));

        when(registroRepository.save(any(RegistroAuditoria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        auditoriaService.registrarAccion(
                " incidente ",
                " usuario-123 ",
                " CREAR_INCIDENTE ",
                " INCIDENTE ",
                " incidente-456 ",
                " evento-789 ",
                " MS05 ",
                " EXITOSO ",
                " detalle "
        );

        ArgumentCaptor<RegistroAuditoria> captor =
                ArgumentCaptor.forClass(RegistroAuditoria.class);

        verify(registroRepository).save(captor.capture());

        RegistroAuditoria registro = captor.getValue();

        assertEquals("usuario-123", registro.getUsuarioOrigen());
        assertEquals("CREAR_INCIDENTE", registro.getAccion());
        assertEquals("INCIDENTE", registro.getRecursoTipo());
        assertEquals("incidente-456", registro.getRecursoId());
        assertEquals("evento-789", registro.getEventoId());
        assertEquals("MS05", registro.getOrigen());
        assertEquals("EXITOSO", registro.getResultado());
        assertEquals("detalle", registro.getDetalle());
    }

    @Test
    void debePermitirCamposOpcionalesVacios() {

        when(tipoRepository.findByNombre("INCIDENTE"))
                .thenReturn(Optional.of(tipoIncidente));

        when(registroRepository.save(any(RegistroAuditoria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        auditoriaService.registrarAccion(
                "INCIDENTE",
                "usuario-123",
                "CREAR_INCIDENTE",
                "INCIDENTE",
                "incidente-456",
                " ",
                "MS05",
                "EXITOSO",
                null
        );

        ArgumentCaptor<RegistroAuditoria> captor =
                ArgumentCaptor.forClass(RegistroAuditoria.class);

        verify(registroRepository).save(captor.capture());

        RegistroAuditoria registro = captor.getValue();

        assertNull(registro.getEventoId());
        assertNull(registro.getDetalle());
    }

    @Test
    void debeRechazarCamposObligatoriosVacios() {

        assertThrows(IllegalArgumentException.class, () ->
                registrar(null, "usuario", "ACCION", "INCIDENTE",
                        "123", "evento", "MS05", "EXITOSO"));

        assertThrows(IllegalArgumentException.class, () ->
                registrar("INCIDENTE", null, "ACCION", "INCIDENTE",
                        "123", "evento", "MS05", "EXITOSO"));

        assertThrows(IllegalArgumentException.class, () ->
                registrar("INCIDENTE", "usuario", " ", "INCIDENTE",
                        "123", "evento", "MS05", "EXITOSO"));

        assertThrows(IllegalArgumentException.class, () ->
                registrar("INCIDENTE", "usuario", "ACCION", null,
                        "123", "evento", "MS05", "EXITOSO"));

        assertThrows(IllegalArgumentException.class, () ->
                registrar("INCIDENTE", "usuario", "ACCION", "INCIDENTE",
                        " ", "evento", "MS05", "EXITOSO"));

        assertThrows(IllegalArgumentException.class, () ->
                registrar("INCIDENTE", "usuario", "ACCION", "INCIDENTE",
                        "123", "evento", " ", "EXITOSO"));

        assertThrows(IllegalArgumentException.class, () ->
                registrar("INCIDENTE", "usuario", "ACCION", "INCIDENTE",
                        "123", "evento", "MS05", null));

        verifyNoInteractions(tipoRepository);
        verifyNoInteractions(registroRepository);
    }

    @Test
    void debeRechazarUnTipoDeRegistroInexistente() {

        when(tipoRepository.findByNombre("DESCONOCIDO"))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () ->
                auditoriaService.registrarAccion(
                        "DESCONOCIDO",
                        "usuario-123",
                        "ACCION",
                        "INCIDENTE",
                        "123",
                        null,
                        "MS05",
                        "EXITOSO",
                        null
                ));

        verify(registroRepository, never()).save(any());
    }

    // =====================================================
    // 2. CONSULTAS DE AUDITORIA
    // =====================================================

    @Test
    void debeConsultarRegistrosPorEvento() {

        RegistroAuditoria registro = new RegistroAuditoria();

        when(registroRepository
                .findByEventoIdOrderByFechaHoraRegistroAsc("evento-123"))
                .thenReturn(List.of(registro));

        List<RegistroAuditoria> resultado =
                auditoriaService.consultarPorEvento("evento-123");

        assertEquals(1, resultado.size());

        verify(registroRepository)
                .findByEventoIdOrderByFechaHoraRegistroAsc("evento-123");
    }

    @Test
    void debeRetornarListaVaciaCuandoElEventoNoTieneAuditoria() {

        when(registroRepository
                .findByEventoIdOrderByFechaHoraRegistroAsc("evento-123"))
                .thenReturn(List.of());

        List<RegistroAuditoria> resultado =
                auditoriaService.consultarPorEvento("evento-123");

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void debeConsultarRegistrosPorRecurso() {

        RegistroAuditoria registro = new RegistroAuditoria();

        when(registroRepository
                .findByRecursoTipoAndRecursoIdOrderByFechaHoraRegistroAsc(
                        "INCIDENTE",
                        "incidente-123"
                ))
                .thenReturn(List.of(registro));

        List<RegistroAuditoria> resultado =
                auditoriaService.consultarPorRecurso(
                        "INCIDENTE",
                        "incidente-123"
                );

        assertEquals(1, resultado.size());

        verify(registroRepository)
                .findByRecursoTipoAndRecursoIdOrderByFechaHoraRegistroAsc(
                        "INCIDENTE",
                        "incidente-123"
                );
    }

    @Test
    void debeRetornarListaVaciaCuandoElRecursoNoTieneAuditoria() {

        when(registroRepository
                .findByRecursoTipoAndRecursoIdOrderByFechaHoraRegistroAsc(
                        "INCIDENTE",
                        "incidente-123"
                ))
                .thenReturn(List.of());

        List<RegistroAuditoria> resultado =
                auditoriaService.consultarPorRecurso(
                        "INCIDENTE",
                        "incidente-123"
                );

        assertTrue(resultado.isEmpty());
    }

    @Test
    void debeConsultarTodosLosRegistrosEnOrdenCronologico() {

        RegistroAuditoria primero = new RegistroAuditoria();
        RegistroAuditoria segundo = new RegistroAuditoria();

        when(registroRepository.findAllByOrderByFechaHoraRegistroAsc())
                .thenReturn(List.of(primero, segundo));

        List<RegistroAuditoria> resultado =
                auditoriaService.consultarTodos();

        assertEquals(2, resultado.size());
        assertSame(primero, resultado.get(0));
        assertSame(segundo, resultado.get(1));

        verify(registroRepository)
                .findAllByOrderByFechaHoraRegistroAsc();
    }

    @Test
    void debeRetornarListaVaciaAlConsultarTodosSinRegistros() {

        when(registroRepository.findAllByOrderByFechaHoraRegistroAsc())
                .thenReturn(List.of());

        List<RegistroAuditoria> resultado =
                auditoriaService.consultarTodos();

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void debeValidarLosParametrosDeConsulta() {

        assertThrows(IllegalArgumentException.class, () ->
                auditoriaService.consultarPorEvento(" "));

        assertThrows(IllegalArgumentException.class, () ->
                auditoriaService.consultarPorRecurso(null, "123"));

        assertThrows(IllegalArgumentException.class, () ->
                auditoriaService.consultarPorRecurso("INCIDENTE", " "));

        verifyNoInteractions(registroRepository);
    }

    // =====================================================
    // 3. RECEPCION DE EVENTOS DE ASIGNACIONES
    // =====================================================

    @Test
    void debeConvertirEventoDeAsignacionEnRegistroDeAuditoria() {

        UUID asignacionId = UUID.randomUUID();

        AsignacionAuditEvent evento = new AsignacionAuditEvent(
                asignacionId,
                "CREAR_ASIGNACION",
                "coordinador-123",
                "brigadista-456",
                "evento-789",
                LocalDateTime.now()
        );

        listener.escucharAsignacion(evento);

        verify(auditoriaServiceMock).registrarAccion(
                eq("ASIGNACION"),
                eq("coordinador-123"),
                eq("CREAR_ASIGNACION"),
                eq("ASIGNACION"),
                eq(asignacionId.toString()),
                eq("evento-789"),
                eq("MS05"),
                eq("EXITOSO"),
                eq("Accion realizada sobre la asignacion del brigadista: brigadista-456"),
                any(OffsetDateTime.class)
        );
    }

    // =====================================================
    // 4. RECEPCION DE EVENTOS DE INCIDENTES Y EVIDENCIAS
    // =====================================================

    @Test
    void debeConvertirEventoDeIncidenteEnRegistroDeAuditoria() {

        OffsetDateTime fechaEvento = OffsetDateTime.now();

        IncidenteAuditEvent evento = new IncidenteAuditEvent(
                "CREAR_INCIDENTE",
                "INCIDENTE",
                "incidente-123",
                "evento-456",
                "usuario-789",
                "EXITOSO",
                "Incidente creado",
                fechaEvento
        );

        listener.escucharIncidente(evento);

        verify(auditoriaServiceMock).registrarAccion(
                eq("INCIDENTE"),
                eq("usuario-789"),
                eq("CREAR_INCIDENTE"),
                eq("INCIDENTE"),
                eq("incidente-123"),
                eq("evento-456"),
                eq("MS05"),
                eq("EXITOSO"),
                eq("Incidente creado"),
                eq(fechaEvento)
        );
    }

    @Test
    void debeClasificarLaEvidenciaComoTipoEvidencia() {

        OffsetDateTime fechaEvento = OffsetDateTime.now();

        IncidenteAuditEvent evento = new IncidenteAuditEvent(
                "CARGAR_EVIDENCIA",
                "EVIDENCIA_INCIDENTE",
                "evidencia-123",
                "evento-456",
                "usuario-789",
                "EXITOSO",
                "Evidencia cargada",
                fechaEvento
        );

        listener.escucharIncidente(evento);

        verify(auditoriaServiceMock).registrarAccion(
                eq("EVIDENCIA"),
                eq("usuario-789"),
                eq("CARGAR_EVIDENCIA"),
                eq("EVIDENCIA_INCIDENTE"),
                eq("evidencia-123"),
                eq("evento-456"),
                eq("MS05"),
                eq("EXITOSO"),
                eq("Evidencia cargada"),
                eq(fechaEvento)
        );
    }

    // =====================================================
    // METODO AUXILIAR
    // =====================================================

    private void registrar(
            String tipo,
            String usuario,
            String accion,
            String recursoTipo,
            String recursoId,
            String eventoId,
            String origen,
            String resultado
    ) {
        auditoriaService.registrarAccion(
                tipo,
                usuario,
                accion,
                recursoTipo,
                recursoId,
                eventoId,
                origen,
                resultado,
                null
        );
    }
}
