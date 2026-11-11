
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.client.UbicacionClient;
import edu.eci.dosw.brigades_and_incidents.dto.CrearAsignacionRequest;
import edu.eci.dosw.brigades_and_incidents.dto.ModificarAsignacionRequest;
import edu.eci.dosw.brigades_and_incidents.entity.AsignacionBrigadista;
import edu.eci.dosw.brigades_and_incidents.entity.EstadoAsignacion;
import edu.eci.dosw.brigades_and_incidents.entity.EstadoIncidente;
import edu.eci.dosw.brigades_and_incidents.entity.Incidente;
import edu.eci.dosw.brigades_and_incidents.entity.TipoUbicacion;
import edu.eci.dosw.brigades_and_incidents.event.AsignacionAuditEvent;
import edu.eci.dosw.brigades_and_incidents.exception.ConflictoOperacionException;
import edu.eci.dosw.brigades_and_incidents.exception.RecursoNoEncontradoException;
import edu.eci.dosw.brigades_and_incidents.repository.AsignacionBrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.EstadoAsignacionRepository;
import edu.eci.dosw.brigades_and_incidents.repository.IncidenteRepository;
import edu.eci.dosw.brigades_and_incidents.repository.TipoUbicacionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsignacionBrigadistasServiceTest {

    @Mock
    private AsignacionBrigadistaRepository asignacionRepository;

    @Mock
    private EstadoAsignacionRepository estadoRepository;

    @Mock
    private TipoUbicacionRepository tipoRepository;

    @Mock
    private IncidenteRepository incidenteRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UbicacionClient ubicacionClient;

    @InjectMocks
    private AsignacionBrigadistasService service;

    private EstadoAsignacion vigente;
    private EstadoAsignacion retirada;
    private TipoUbicacion zonaSegura;

    @BeforeEach
    void setUp() {
        vigente = mock(EstadoAsignacion.class);
        retirada = mock(EstadoAsignacion.class);
        zonaSegura = mock(TipoUbicacion.class);

        lenient().when(vigente.getNombre()).thenReturn("VIGENTE");
        lenient().when(retirada.getNombre()).thenReturn("RETIRADA");
        lenient().when(zonaSegura.getNombre()).thenReturn("ZONA_SEGURA");

        // Simula una ubicación existente y activa en MS02.
        lenient().when(
                ubicacionClient.consultarUbicacion(
                        anyString(),
                        anyString()
                )
        ).thenAnswer(invocation -> new UbicacionClient.UbicacionExterna(
                invocation.getArgument(0),
                "Ubicación de prueba",
                invocation.getArgument(1),
                true
        ));
    }

    // --------------------------------------------------
    // CREACIÓN DE ASIGNACIONES
    // --------------------------------------------------

    @Test
    void debeCrearAsignacionCorrectamente() {
        CrearAsignacionRequest request = new CrearAsignacionRequest(
                "BRIG-001",
                "EVENTO-001",
                "ZONA-001",
                "ZONA_SEGURA",
                "Asignación de prueba"
        );

        UUID id = UUID.randomUUID();
        AsignacionBrigadista guardada = mock(AsignacionBrigadista.class);

        when(tipoRepository.findByNombre("ZONA_SEGURA"))
                .thenReturn(Optional.of(zonaSegura));

        when(asignacionRepository
                .existsByBrigadistaIdAndEventoIdAndUbicacionIdAndTipoUbicacion_NombreAndEstado_Nombre(
                        "BRIG-001",
                        "EVENTO-001",
                        "ZONA-001",
                        "ZONA_SEGURA",
                        "VIGENTE"
                )).thenReturn(false);

        when(estadoRepository.findByNombre("VIGENTE"))
                .thenReturn(Optional.of(vigente));

        when(asignacionRepository.save(any(AsignacionBrigadista.class)))
                .thenReturn(guardada);

        when(guardada.getIdAsignacion()).thenReturn(id);
        when(guardada.getBrigadistaId()).thenReturn("BRIG-001");
        when(guardada.getEventoId()).thenReturn("EVENTO-001");
        when(guardada.getUbicacionId()).thenReturn("ZONA-001");
        when(guardada.getTipoUbicacion()).thenReturn(zonaSegura);
        when(guardada.getEstado()).thenReturn(vigente);
        when(guardada.getObservaciones()).thenReturn("Asignación de prueba");

        var response = service.crearAsignacion(request, "COORD-001");

        assertNotNull(response);
        assertEquals(id, response.idAsignacion());
        assertEquals("BRIG-001", response.brigadistaId());
        assertEquals("EVENTO-001", response.eventoId());
        assertEquals("ZONA-001", response.ubicacionId());
        assertEquals("ZONA_SEGURA", response.tipoUbicacion());
        assertEquals("VIGENTE", response.estado());

        verify(ubicacionClient).consultarUbicacion(
                "ZONA-001",
                "ZONA_SEGURA"
        );
        verify(asignacionRepository).save(any(AsignacionBrigadista.class));
        verify(eventPublisher).publishEvent(any(AsignacionAuditEvent.class));
    }

    @Test
    void noDebeCrearUnaAsignacionDuplicada() {
        CrearAsignacionRequest request = new CrearAsignacionRequest(
                "BRIG-001",
                "EVENTO-001",
                "ZONA-001",
                "ZONA_SEGURA",
                "Prueba de duplicado"
        );

        when(tipoRepository.findByNombre("ZONA_SEGURA"))
                .thenReturn(Optional.of(zonaSegura));

        when(asignacionRepository
                .existsByBrigadistaIdAndEventoIdAndUbicacionIdAndTipoUbicacion_NombreAndEstado_Nombre(
                        "BRIG-001",
                        "EVENTO-001",
                        "ZONA-001",
                        "ZONA_SEGURA",
                        "VIGENTE"
                )).thenReturn(true);

        assertThrows(
                ConflictoOperacionException.class,
                () -> service.crearAsignacion(request, "COORD-001")
        );

        verify(asignacionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void noDebeCrearAsignacionConUbicacionInexistente() {
        CrearAsignacionRequest request = new CrearAsignacionRequest(
                "BRIG-001",
                "EVENTO-001",
                "ZONA-999",
                "ZONA_SEGURA",
                "Ubicación inexistente"
        );

        when(tipoRepository.findByNombre("ZONA_SEGURA"))
                .thenReturn(Optional.of(zonaSegura));

        when(ubicacionClient.consultarUbicacion(
                "ZONA-999",
                "ZONA_SEGURA"
        )).thenReturn(null);

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> service.crearAsignacion(request, "COORD-001")
        );

        verify(asignacionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void noDebeCrearAsignacionEnUbicacionInactiva() {
        CrearAsignacionRequest request = new CrearAsignacionRequest(
                "BRIG-001",
                "EVENTO-001",
                "ZONA-002",
                "ZONA_SEGURA",
                "Ubicación inactiva"
        );

        when(tipoRepository.findByNombre("ZONA_SEGURA"))
                .thenReturn(Optional.of(zonaSegura));

        when(ubicacionClient.consultarUbicacion(
                "ZONA-002",
                "ZONA_SEGURA"
        )).thenReturn(new UbicacionClient.UbicacionExterna(
                "ZONA-002",
                "Zona inactiva",
                "ZONA_SEGURA",
                false
        ));

        assertThrows(
                ConflictoOperacionException.class,
                () -> service.crearAsignacion(request, "COORD-001")
        );

        verify(asignacionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void noDebeCrearAsignacionSinIdentificadorDeActor() {
        CrearAsignacionRequest request = new CrearAsignacionRequest(
                "BRIG-001",
                "EVENTO-001",
                "ZONA-001",
                "ZONA_SEGURA",
                null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.crearAsignacion(request, " ")
        );

        verifyNoInteractions(tipoRepository);
        verify(asignacionRepository, never()).save(any());
    }

    // --------------------------------------------------
    // MODIFICACIÓN DE ASIGNACIONES
    // --------------------------------------------------

    @Test
    void noDebeModificarUnaAsignacionRetirada() {
        UUID id = UUID.randomUUID();
        AsignacionBrigadista asignacion = mock(AsignacionBrigadista.class);

        when(asignacionRepository.findById(id))
                .thenReturn(Optional.of(asignacion));

        when(asignacion.getEstado()).thenReturn(retirada);

        assertThrows(
                ConflictoOperacionException.class,
                () -> service.modificarAsignacion(
                        id,
                        new ModificarAsignacionRequest(
                                "ZONA-002",
                                "ZONA_SEGURA",
                                "Cambio de ubicación"
                        ),
                        "COORD-001"
                )
        );

        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void noDebeCambiarUbicacionDelUltimoBrigadistaConIncidentesPendientes() {
        UUID id = UUID.randomUUID();
        AsignacionBrigadista asignacion = prepararAsignacion(id);

        Incidente incidente = mock(Incidente.class);
        EstadoIncidente estadoIncidente = mock(EstadoIncidente.class);

        when(incidente.getEstado()).thenReturn(estadoIncidente);
        when(estadoIncidente.getNombre()).thenReturn("EN_ATENCION");

        when(incidenteRepository.findByEventoIdAndUbicacionId(
                "EVENTO-001",
                "ZONA-001"
        )).thenReturn(List.of(incidente));

        when(asignacionRepository.findByEventoIdAndEstado_Nombre(
                "EVENTO-001",
                "VIGENTE"
        )).thenReturn(List.of(asignacion));

        when(tipoRepository.findByNombre("ZONA_SEGURA"))
                .thenReturn(Optional.of(zonaSegura));

        assertThrows(
                ConflictoOperacionException.class,
                () -> service.modificarAsignacion(
                        id,
                        new ModificarAsignacionRequest(
                                "ZONA-002",
                                "ZONA_SEGURA",
                                "Cambio de zona"
                        ),
                        "COORD-001"
                )
        );

        verify(asignacionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // --------------------------------------------------
    // RETIRO DE ASIGNACIONES
    // --------------------------------------------------

    @Test
    void noDebeRetirarAlUltimoBrigadistaConIncidentesPendientes() {
        UUID id = UUID.randomUUID();
        AsignacionBrigadista asignacion = prepararAsignacion(id);

        Incidente incidente = mock(Incidente.class);
        EstadoIncidente estadoIncidente = mock(EstadoIncidente.class);

        when(incidente.getEstado()).thenReturn(estadoIncidente);
        when(estadoIncidente.getNombre()).thenReturn("EN_ATENCION");

        when(incidenteRepository.findByEventoIdAndUbicacionId(
                "EVENTO-001",
                "ZONA-001"
        )).thenReturn(List.of(incidente));

        when(asignacionRepository.findByEventoIdAndEstado_Nombre(
                "EVENTO-001",
                "VIGENTE"
        )).thenReturn(List.of());

        assertThrows(
                ConflictoOperacionException.class,
                () -> service.retirarAsignacion(id, "COORD-001")
        );

        verify(asignacionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void debePermitirRetirarSiExisteOtroBrigadistaEnLaZona() {
        UUID id = UUID.randomUUID();
        AsignacionBrigadista asignacion = prepararAsignacion(id);

        AsignacionBrigadista otroBrigadista =
                mock(AsignacionBrigadista.class);

        when(otroBrigadista.getIdAsignacion())
                .thenReturn(UUID.randomUUID());
        when(otroBrigadista.getUbicacionId())
                .thenReturn("ZONA-001");
        when(otroBrigadista.getTipoUbicacion())
                .thenReturn(zonaSegura);

        Incidente incidente = mock(Incidente.class);
        EstadoIncidente estadoIncidente = mock(EstadoIncidente.class);

        when(incidente.getEstado()).thenReturn(estadoIncidente);
        when(estadoIncidente.getNombre()).thenReturn("EN_ATENCION");

        when(incidenteRepository.findByEventoIdAndUbicacionId(
                "EVENTO-001",
                "ZONA-001"
        )).thenReturn(List.of(incidente));

        when(asignacionRepository.findByEventoIdAndEstado_Nombre(
                "EVENTO-001",
                "VIGENTE"
        )).thenReturn(List.of(otroBrigadista));

        when(estadoRepository.findByNombre("RETIRADA"))
                .thenReturn(Optional.of(retirada));

        when(asignacionRepository.save(asignacion))
                .thenReturn(asignacion);

        var response = service.retirarAsignacion(id, "COORD-001");

        assertNotNull(response);
        assertEquals("RETIRADA", response.estado());

        verify(asignacionRepository).save(asignacion);
        verify(eventPublisher).publishEvent(any(AsignacionAuditEvent.class));
    }

    @Test
    void debePermitirRetirarCuandoLosIncidentesEstanResueltos() {
        UUID id = UUID.randomUUID();
        AsignacionBrigadista asignacion = prepararAsignacion(id);

        Incidente incidente = mock(Incidente.class);
        EstadoIncidente estadoIncidente = mock(EstadoIncidente.class);

        when(incidente.getEstado()).thenReturn(estadoIncidente);
        when(estadoIncidente.getNombre()).thenReturn("RESUELTO");

        when(incidenteRepository.findByEventoIdAndUbicacionId(
                "EVENTO-001",
                "ZONA-001"
        )).thenReturn(List.of(incidente));

        when(estadoRepository.findByNombre("RETIRADA"))
                .thenReturn(Optional.of(retirada));

        when(asignacionRepository.save(asignacion))
                .thenReturn(asignacion);

        var response = service.retirarAsignacion(id, "COORD-001");

        assertNotNull(response);
        assertEquals("RETIRADA", response.estado());

        verify(asignacionRepository).save(asignacion);
        verify(eventPublisher).publishEvent(any(AsignacionAuditEvent.class));
    }

    // --------------------------------------------------
    // MÉTODO AUXILIAR
    // --------------------------------------------------

    private AsignacionBrigadista prepararAsignacion(UUID id) {
        AsignacionBrigadista asignacion =
                mock(AsignacionBrigadista.class);

        lenient().when(asignacionRepository.findById(id))
                .thenReturn(Optional.of(asignacion));

        lenient().when(asignacion.getIdAsignacion()).thenReturn(id);
        lenient().when(asignacion.getBrigadistaId()).thenReturn("BRIG-001");
        lenient().when(asignacion.getEventoId()).thenReturn("EVENTO-001");
        lenient().when(asignacion.getUbicacionId()).thenReturn("ZONA-001");
        lenient().when(asignacion.getTipoUbicacion()).thenReturn(zonaSegura);
        lenient().when(asignacion.getObservaciones()).thenReturn("Prueba");

        lenient().when(asignacion.getEstado())
                .thenReturn(vigente, retirada);

        return asignacion;
    }
}
