package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.entity.EstadoIncidente;
import edu.eci.dosw.brigades_and_incidents.entity.HistorialIncidente;
import edu.eci.dosw.brigades_and_incidents.entity.Incidente;
import edu.eci.dosw.brigades_and_incidents.event.IncidenteAuditPublisher;
import edu.eci.dosw.brigades_and_incidents.repository.EstadoIncidenteRepository;
import edu.eci.dosw.brigades_and_incidents.repository.HistorialIncidenteRepository;
import edu.eci.dosw.brigades_and_incidents.repository.IncidenteRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReclamoIncidentesZonaServiceTest {

    private static final String EVENTO = "EVENTO-1";
    private static final String BRIGADISTA =
            "22222222-2222-2222-2222-222222222222";
    private static final String ZONA = "ZONA-1";

    private IncidenteRepository incidenteRepository;
    private EstadoIncidenteRepository estadoRepository;
    private HistorialIncidenteRepository historialRepository;
    private IncidenteAuditPublisher auditPublisher;

    private ReclamoIncidentesZonaService service;

    private final EstadoIncidente reportado = new EstadoIncidente("REPORTADO", "");
    private final EstadoIncidente asignado = new EstadoIncidente("ASIGNADO", "");

    @BeforeEach
    void setUp() {
        incidenteRepository = mock(IncidenteRepository.class);
        estadoRepository = mock(EstadoIncidenteRepository.class);
        historialRepository = mock(HistorialIncidenteRepository.class);
        auditPublisher = mock(IncidenteAuditPublisher.class);

        service = new ReclamoIncidentesZonaService(
                incidenteRepository,
                estadoRepository,
                historialRepository,
                auditPublisher
        );

        when(estadoRepository.findByNombre("ASIGNADO"))
                .thenReturn(Optional.of(asignado));
        when(incidenteRepository.save(any(Incidente.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void reclamaLosIncidentesReportadosSinResponsableDeLaZona() {
        Incidente pendiente = incidente(reportado, null);
        Incidente asignadoAOtro = incidente(asignado, null);
        Incidente conResponsable = incidente(reportado, "OTRO-BRIGADISTA");
        Incidente eliminado = incidente(reportado, null);
        eliminado.setEliminado(true);

        when(incidenteRepository.findByEventoIdAndUbicacionId(EVENTO, ZONA))
                .thenReturn(List.of(pendiente, asignadoAOtro, conResponsable, eliminado));

        int reclamados = service.reclamarReportados(EVENTO, BRIGADISTA, ZONA);

        assertEquals(1, reclamados);

        assertSame(asignado, pendiente.getEstado());
        assertEquals(BRIGADISTA, pendiente.getBrigadistaResponsableId());

        // Los demás no cambian
        assertSame(asignado, asignadoAOtro.getEstado());
        assertEquals("OTRO-BRIGADISTA", conResponsable.getBrigadistaResponsableId());
        assertEquals(reportado, eliminado.getEstado());

        ArgumentCaptor<HistorialIncidente> historial =
                ArgumentCaptor.forClass(HistorialIncidente.class);
        verify(historialRepository).save(historial.capture());

        assertSame(pendiente, historial.getValue().getIncidente());
        assertSame(reportado, historial.getValue().getEstadoAnterior());
        assertSame(asignado, historial.getValue().getEstadoNuevo());
        assertEquals(BRIGADISTA, historial.getValue().getResponsableNuevoId());
        assertEquals(BRIGADISTA, historial.getValue().getActorId());

        verify(auditPublisher).publicar(
                eq("ASIGNAR_RESPONSABLE"),
                eq("INCIDENTE"),
                eq(pendiente.getId().toString()),
                eq(EVENTO),
                eq(BRIGADISTA),
                eq("EXITOSO"),
                anyString(),
                anyMap()
        );
    }

    @Test
    void sinIncidentesReportadosNoCambiaNada() {
        when(incidenteRepository.findByEventoIdAndUbicacionId(EVENTO, ZONA))
                .thenReturn(List.of());

        int reclamados = service.reclamarReportados(EVENTO, BRIGADISTA, ZONA);

        assertEquals(0, reclamados);
        verify(incidenteRepository, never()).save(any());
        verify(historialRepository, never()).save(any());
        verify(auditPublisher, never()).publicar(
                anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyMap());
    }

    @Test
    void sinDatosObligatoriosNoConsultaLaBase() {
        assertEquals(0, service.reclamarReportados(null, BRIGADISTA, ZONA));
        assertEquals(0, service.reclamarReportados(EVENTO, " ", ZONA));
        assertEquals(0, service.reclamarReportados(EVENTO, BRIGADISTA, null));

        verify(incidenteRepository, never())
                .findByEventoIdAndUbicacionId(anyString(), anyString());
    }

    private Incidente incidente(EstadoIncidente estado, String responsable) {
        Incidente incidente = new Incidente();
        incidente.setId(UUID.randomUUID());
        incidente.setEventoId(EVENTO);
        incidente.setUbicacionId(ZONA);
        incidente.setEstado(estado);
        incidente.setBrigadistaResponsableId(responsable);
        return incidente;
    }
}
