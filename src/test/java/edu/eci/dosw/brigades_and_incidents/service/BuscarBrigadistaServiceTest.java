
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.client.BrigadistaClient;
import edu.eci.dosw.brigades_and_incidents.client.BrigadistaClient.BrigadistaExterno;
import edu.eci.dosw.brigades_and_incidents.dto.BusquedaBrigadistasResponse;
import edu.eci.dosw.brigades_and_incidents.entity.Brigadista;
import edu.eci.dosw.brigades_and_incidents.entity.EstadoBrigadista;
import edu.eci.dosw.brigades_and_incidents.exception.Ms01IntegrationException;
import edu.eci.dosw.brigades_and_incidents.mapper.BrigadistaBusquedaMapper;
import edu.eci.dosw.brigades_and_incidents.repository.AsignacionBrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.BrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.EstadoBrigadistaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BuscarBrigadistaServiceTest {

    private BrigadistaClient brigadistaClient;
    private BrigadistaRepository brigadistaRepository;
    private EstadoBrigadistaRepository estadoRepository;
    private AsignacionBrigadistaRepository asignacionRepository;
    private BrigadistaBusquedaMapper mapper;

    private BuscarBrigadistaService service;

    private EstadoBrigadista estadoActivo;
    private EstadoBrigadista estadoInactivo;

    @BeforeEach
    void setUp() {
        brigadistaClient = mock(BrigadistaClient.class);
        brigadistaRepository = mock(BrigadistaRepository.class);
        estadoRepository = mock(EstadoBrigadistaRepository.class);
        asignacionRepository = mock(AsignacionBrigadistaRepository.class);
        mapper = mock(BrigadistaBusquedaMapper.class);

        service = new BuscarBrigadistaService(
                brigadistaClient,
                brigadistaRepository,
                estadoRepository,
                asignacionRepository,
                mapper
        );

        estadoActivo = new EstadoBrigadista("ACTIVO", "Activo");
        estadoInactivo = new EstadoBrigadista("INACTIVO", "Inactivo");
    }

    @Test
    void debeRechazarBusquedaNula() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.buscarBrigadistas(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(brigadistaClient);
    }

    @Test
    void debeRechazarBusquedaVacia() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.buscarBrigadistas("   ")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(brigadistaClient);
    }

    @Test
    void debeInformarServicioNoDisponibleCuandoMs01Falla() {
        when(brigadistaClient.listarBrigadistas())
                .thenThrow(new Ms01IntegrationException("Error de conexión"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.buscarBrigadistas("Ana")
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getStatusCode()
        );
        verifyNoInteractions(estadoRepository);
    }

    @Test
    void debeInformarServicioNoDisponibleCuandoMs01RetornaNulo() {
        when(brigadistaClient.listarBrigadistas()).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.buscarBrigadistas("Ana")
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getStatusCode()
        );
        verifyNoInteractions(estadoRepository);
    }

    @Test
    void debeFallarCuandoNoExisteEstadoInicialInactivo() {
        when(brigadistaClient.listarBrigadistas()).thenReturn(List.of());
        when(estadoRepository.findByNombre("INACTIVO"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> service.buscarBrigadistas("Ana")
        );

        verify(brigadistaRepository, never()).save(any());
    }

    @Test
    void debeBuscarBrigadistasOrdenadosYFiltradosPorNombre() {
        List<BrigadistaExterno> externos = List.of(
                new BrigadistaExterno("id-2", "Ana María", "BRIGADISTA"),
                new BrigadistaExterno("id-1", "Ana", "BRIGADISTA"),
                new BrigadistaExterno("id-3", "Ana Coordinadora", "COORDINATOR"),
                new BrigadistaExterno("id-4", "Carlos", "BRIGADISTA")
        );

        Brigadista ana = new Brigadista("id-1", estadoActivo);
        Brigadista anaMaria = new Brigadista("id-2", estadoInactivo);
        Brigadista carlos = new Brigadista("id-4", estadoActivo);

        when(brigadistaClient.listarBrigadistas()).thenReturn(externos);

        when(estadoRepository.findByNombre("INACTIVO"))
                .thenReturn(Optional.of(estadoInactivo));

        when(brigadistaRepository.findByUsuarioIdExterno("id-1"))
                .thenReturn(Optional.of(ana));

        when(brigadistaRepository.findByUsuarioIdExterno("id-2"))
                .thenReturn(Optional.of(anaMaria));

        when(brigadistaRepository.findByUsuarioIdExterno("id-4"))
                .thenReturn(Optional.of(carlos));

        when(asignacionRepository.findByBrigadistaIdAndEstado_Nombre(
                any(), eq("VIGENTE")
        )).thenReturn(List.of());

        when(mapper.convertirAsignaciones(any())).thenReturn(List.of());

        BusquedaBrigadistasResponse response =
                service.buscarBrigadistas(" ANA ");

        assertEquals("Brigadistas encontrados", response.mensaje());
        assertEquals(2, response.resultados().size());

        assertEquals("id-1", response.resultados().get(0).idBrigadista());
        assertEquals("Ana", response.resultados().get(0).nombre());
        assertEquals("ACTIVO", response.resultados().get(0).estado());

        assertEquals("id-2", response.resultados().get(1).idBrigadista());
        assertEquals("Ana María", response.resultados().get(1).nombre());
        assertEquals("INACTIVO", response.resultados().get(1).estado());

        verify(brigadistaRepository, never()).save(any());
        verify(mapper, times(2)).convertirAsignaciones(any());
    }

    @Test
    void debeRegistrarLocalmenteBrigadistaQueAunNoExiste() {
        BrigadistaExterno externo =
                new BrigadistaExterno("nuevo-1", "Ana Pérez", "BRIGADISTA");

        Brigadista brigadistaGuardado =
                new Brigadista("nuevo-1", estadoInactivo);

        when(brigadistaClient.listarBrigadistas())
                .thenReturn(List.of(externo));

        when(estadoRepository.findByNombre("INACTIVO"))
                .thenReturn(Optional.of(estadoInactivo));

        when(brigadistaRepository.findByUsuarioIdExterno("nuevo-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(brigadistaGuardado));

        when(brigadistaRepository.save(any(Brigadista.class)))
                .thenReturn(brigadistaGuardado);

        when(asignacionRepository.findByBrigadistaIdAndEstado_Nombre(
                "nuevo-1", "VIGENTE"
        )).thenReturn(List.of());

        when(mapper.convertirAsignaciones(any())).thenReturn(List.of());

        BusquedaBrigadistasResponse response =
                service.buscarBrigadistas("Ana");

        assertEquals(1, response.resultados().size());
        assertEquals("nuevo-1", response.resultados().get(0).idBrigadista());
        assertEquals("INACTIVO", response.resultados().get(0).estado());

        verify(brigadistaRepository).save(any(Brigadista.class));
    }

    @Test
    void debeIgnorarUsuariosQueNoSeanBrigadistas() {
        List<BrigadistaExterno> externos = List.of(
                new BrigadistaExterno("coord-1", "Ana Coordinadora", "COORDINATOR"),
                new BrigadistaExterno("aud-1", "Ana Auditora", "AUDITOR")
        );

        when(brigadistaClient.listarBrigadistas()).thenReturn(externos);

        when(estadoRepository.findByNombre("INACTIVO"))
                .thenReturn(Optional.of(estadoInactivo));

        BusquedaBrigadistasResponse response =
                service.buscarBrigadistas("Ana");

        assertTrue(response.resultados().isEmpty());

        assertEquals(
                "No hay resultados compatibles con la búsqueda",
                response.mensaje()
        );

        verify(brigadistaRepository, never()).save(any());
        verify(brigadistaRepository, never()).findByUsuarioIdExterno(any());
    }

    @Test
    void debeIgnorarRegistrosExternosNulosOIncompletos() {
        List<BrigadistaExterno> externos = java.util.Arrays.asList(
                null,
                new BrigadistaExterno(null, "Ana", "BRIGADISTA"),
                new BrigadistaExterno(" ", "Ana", "BRIGADISTA"),
                new BrigadistaExterno("id-1", null, "BRIGADISTA"),
                new BrigadistaExterno("id-2", " ", "BRIGADISTA"),
                new BrigadistaExterno("id-3", "Ana", null)
        );

        when(brigadistaClient.listarBrigadistas()).thenReturn(externos);

        when(estadoRepository.findByNombre("INACTIVO"))
                .thenReturn(Optional.of(estadoInactivo));

        BusquedaBrigadistasResponse response =
                service.buscarBrigadistas("Ana");

        assertTrue(response.resultados().isEmpty());

        assertEquals(
                "No hay resultados compatibles con la búsqueda",
                response.mensaje()
        );

        verify(brigadistaRepository, never()).save(any());
    }

    @Test
    void debeRetornarListaVaciaCuandoNoHayCoincidencias() {
        BrigadistaExterno externo =
                new BrigadistaExterno("id-1", "Carlos López", "BRIGADISTA");

        when(brigadistaClient.listarBrigadistas())
                .thenReturn(List.of(externo));

        when(estadoRepository.findByNombre("INACTIVO"))
                .thenReturn(Optional.of(estadoInactivo));

        when(brigadistaRepository.findByUsuarioIdExterno("id-1"))
                .thenReturn(Optional.of(
                        new Brigadista("id-1", estadoActivo)
                ));

        BusquedaBrigadistasResponse response =
                service.buscarBrigadistas("Ana");

        assertTrue(response.resultados().isEmpty());

        assertEquals(
                "No hay resultados compatibles con la búsqueda",
                response.mensaje()
        );

        verifyNoInteractions(asignacionRepository, mapper);
    }

    @Test
    void debeCambiarEstadoABrigadistaActivo() {
        Brigadista brigadista = new Brigadista("id-1", estadoInactivo);

        when(brigadistaRepository.findByUsuarioIdExterno("id-1"))
                .thenReturn(Optional.of(brigadista));

        when(estadoRepository.findByNombre("ACTIVO"))
                .thenReturn(Optional.of(estadoActivo));

        assertDoesNotThrow(() ->
                service.cambiarEstado("id-1", " activo ")
        );

        assertSame(estadoActivo, brigadista.getEstado());
        verify(brigadistaRepository).save(brigadista);
    }

    @Test
    void debeCambiarEstadoABrigadistaInactivo() {
        Brigadista brigadista = new Brigadista("id-1", estadoActivo);

        when(brigadistaRepository.findByUsuarioIdExterno("id-1"))
                .thenReturn(Optional.of(brigadista));

        when(estadoRepository.findByNombre("INACTIVO"))
                .thenReturn(Optional.of(estadoInactivo));

        assertDoesNotThrow(() ->
                service.cambiarEstado("id-1", "inactivo")
        );

        assertSame(estadoInactivo, brigadista.getEstado());
        verify(brigadistaRepository).save(brigadista);
    }

    @Test
    void debeRechazarIdentificadorNuloAlCambiarEstado() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.cambiarEstado(null, "ACTIVO")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(brigadistaRepository, estadoRepository);
    }

    @Test
    void debeRechazarIdentificadorVacioAlCambiarEstado() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.cambiarEstado(" ", "ACTIVO")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(brigadistaRepository, estadoRepository);
    }

    @Test
    void debeRechazarEstadoNuloAlCambiarEstado() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.cambiarEstado("id-1", null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(brigadistaRepository, estadoRepository);
    }

    @Test
    void debeRechazarEstadoVacioAlCambiarEstado() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.cambiarEstado("id-1", " ")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(brigadistaRepository, estadoRepository);
    }

    @Test
    void debeRechazarEstadoNoPermitidoAlCambiarEstado() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.cambiarEstado("id-1", "SUSPENDIDO")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(brigadistaRepository, estadoRepository);
    }

    @Test
    void debeRetornarNoEncontradoSiElBrigadistaNoExiste() {
        when(brigadistaRepository.findByUsuarioIdExterno("desconocido"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.cambiarEstado("desconocido", "ACTIVO")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(brigadistaRepository, never()).save(any());
    }

    @Test
    void debeFallarSiElEstadoSolicitadoNoExisteEnBaseDeDatos() {
        Brigadista brigadista = new Brigadista("id-1", estadoInactivo);

        when(brigadistaRepository.findByUsuarioIdExterno("id-1"))
                .thenReturn(Optional.of(brigadista));

        when(estadoRepository.findByNombre("ACTIVO"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> service.cambiarEstado("id-1", "ACTIVO")
        );

        verify(brigadistaRepository, never()).save(any());
    }
}
