
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.client.UsuarioClient;
import edu.eci.dosw.brigades_and_incidents.entity.*;
import edu.eci.dosw.brigades_and_incidents.event.IncidenteAuditPublisher;
import edu.eci.dosw.brigades_and_incidents.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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

    private GestionAtencionIncidenteService service;

    private static final UUID INCIDENTE_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String EVENTO_ID = "EVENTO-001";
    private static final String BRIGADISTA_ID = "BRIGADISTA-001";
    private static final String UBICACION_ID = "EDIF-001";

    @BeforeEach
    void configurar() {
        service = new GestionAtencionIncidenteService(
                incidenteRepository,
                estadoIncidenteRepository,
                brigadistaRepository,
                asignacionRepository,
                historialRepository,
                usuarioClient,
                auditPublisher
        );
    }

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
    // ACCESO DE BRIGADISTAS
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
}
