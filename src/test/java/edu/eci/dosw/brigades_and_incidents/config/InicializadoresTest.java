package edu.eci.dosw.brigades_and_incidents.config;

import edu.eci.dosw.brigades_and_incidents.entity.EstadoAsignacion;
import edu.eci.dosw.brigades_and_incidents.entity.EstadoBrigadista;
import edu.eci.dosw.brigades_and_incidents.entity.EstadoIncidente;
import edu.eci.dosw.brigades_and_incidents.entity.PrioridadIncidente;
import edu.eci.dosw.brigades_and_incidents.entity.TipoUbicacion;
import edu.eci.dosw.brigades_and_incidents.repository.EstadoAsignacionRepository;
import edu.eci.dosw.brigades_and_incidents.repository.EstadoBrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.EstadoIncidenteRepository;
import edu.eci.dosw.brigades_and_incidents.repository.PrioridadIncidenteRepository;
import edu.eci.dosw.brigades_and_incidents.repository.TipoUbicacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InicializadoresTest {

    @Mock
    private EstadoBrigadistaRepository estadoBrigadistaRepository;
    @Mock
    private EstadoIncidenteRepository estadoIncidenteRepository;
    @Mock
    private PrioridadIncidenteRepository prioridadRepository;
    @Mock
    private EstadoAsignacionRepository estadoAsignacionRepository;
    @Mock
    private TipoUbicacionRepository tipoUbicacionRepository;

    // ---------- EstadoBrigadistaInicializador ----------

    @Test
    void estadoBrigadistaCreaLosEstadosCuandoNoExisten() {
        when(estadoBrigadistaRepository.findByNombre(anyString()))
                .thenReturn(Optional.empty());

        new EstadoBrigadistaInicializador(estadoBrigadistaRepository).run();

        verify(estadoBrigadistaRepository, times(2))
                .save(any(EstadoBrigadista.class));
    }

    @Test
    void estadoBrigadistaNoDuplicaLosExistentes() {
        when(estadoBrigadistaRepository.findByNombre(anyString()))
                .thenReturn(Optional.of(new EstadoBrigadista("X", "d")));

        new EstadoBrigadistaInicializador(estadoBrigadistaRepository).run();

        verify(estadoBrigadistaRepository, never())
                .save(any(EstadoBrigadista.class));
    }

    // ---------- EstadoIncidenteInicializador ----------

    @Test
    void estadoIncidenteCreaLosEstadosCuandoNoExisten() {
        when(estadoIncidenteRepository.findByNombre(anyString()))
                .thenReturn(Optional.empty());

        new EstadoIncidenteInicializador(estadoIncidenteRepository).run();

        verify(estadoIncidenteRepository, times(2))
                .save(any(EstadoIncidente.class));
    }

    @Test
    void estadoIncidenteNoDuplicaLosExistentes() {
        when(estadoIncidenteRepository.findByNombre(anyString()))
                .thenReturn(Optional.of(new EstadoIncidente("X", "d")));

        new EstadoIncidenteInicializador(estadoIncidenteRepository).run();

        verify(estadoIncidenteRepository, never())
                .save(any(EstadoIncidente.class));
    }

    // ---------- PrioridadIncidenteInicializador ----------

    @Test
    void prioridadCreaLasPrioridadesCuandoNoExisten() {
        when(prioridadRepository.findByNombre(anyString()))
                .thenReturn(Optional.empty());

        new PrioridadIncidenteInicializador(prioridadRepository).run();

        verify(prioridadRepository, times(3))
                .save(any(PrioridadIncidente.class));
    }

    @Test
    void prioridadNoDuplicaLasExistentes() {
        when(prioridadRepository.findByNombre(anyString()))
                .thenReturn(Optional.of(new PrioridadIncidente("X", "d")));

        new PrioridadIncidenteInicializador(prioridadRepository).run();

        verify(prioridadRepository, never())
                .save(any(PrioridadIncidente.class));
    }

    // ---------- CatalogoInicializador ----------

    @Test
    void catalogoCreaEstadosYTiposCuandoNoExisten() throws Exception {
        when(estadoAsignacionRepository.findByNombre(anyString()))
                .thenReturn(Optional.empty());
        when(tipoUbicacionRepository.findByNombre(anyString()))
                .thenReturn(Optional.empty());

        new CatalogoInicializador()
                .inicializarCatalogos(
                        estadoAsignacionRepository, tipoUbicacionRepository)
                .run();

        verify(estadoAsignacionRepository, times(2))
                .save(any(EstadoAsignacion.class));
        verify(tipoUbicacionRepository, times(3))
                .save(any(TipoUbicacion.class));
    }

    @Test
    void catalogoNoDuplicaLosExistentes() throws Exception {
        when(estadoAsignacionRepository.findByNombre(anyString()))
                .thenReturn(Optional.of(new EstadoAsignacion("X", "d")));
        when(tipoUbicacionRepository.findByNombre(anyString()))
                .thenReturn(Optional.of(new TipoUbicacion("X", "d")));

        new CatalogoInicializador()
                .inicializarCatalogos(
                        estadoAsignacionRepository, tipoUbicacionRepository)
                .run();

        verify(estadoAsignacionRepository, never())
                .save(any(EstadoAsignacion.class));
        verify(tipoUbicacionRepository, never())
                .save(any(TipoUbicacion.class));
    }
}