package edu.eci.dosw.brigades_and_incidents.mapper;

import edu.eci.dosw.brigades_and_incidents.dto.AsignacionBrigadistaResumenResponse;
import edu.eci.dosw.brigades_and_incidents.dto.IncidenteResponse;
import edu.eci.dosw.brigades_and_incidents.entity.AsignacionBrigadista;
import edu.eci.dosw.brigades_and_incidents.entity.EstadoAsignacion;
import edu.eci.dosw.brigades_and_incidents.entity.EstadoIncidente;
import edu.eci.dosw.brigades_and_incidents.entity.Incidente;
import edu.eci.dosw.brigades_and_incidents.entity.PrioridadIncidente;
import edu.eci.dosw.brigades_and_incidents.entity.TipoUbicacion;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MappersTest {

    @Test
    void incidenteMapperConvierteTodosLosCampos() {
        UUID id = UUID.randomUUID();
        OffsetDateTime fecha = OffsetDateTime.now();

        Incidente incidente = new Incidente();
        incidente.setId(id);
        incidente.setEventoId("EVT-1");
        incidente.setNombreAfectado("Ana");
        incidente.setReportanteId("USR-1");
        incidente.setUbicacionId("EDIF-001");
        incidente.setTipoUbicacion(new TipoUbicacion("EDIFICIO", "d"));
        incidente.setPrioridad(new PrioridadIncidente("EMERGENCIA", "d"));
        incidente.setEstado(new EstadoIncidente("REPORTADO", "d"));
        incidente.setDescripcion("Caída en escaleras");
        incidente.setFechaReporte(fecha);

        IncidenteResponse respuesta = new IncidenteMapper().toResponse(incidente);

        assertThat(respuesta.idIncidente()).isEqualTo(id);
        assertThat(respuesta.eventoId()).isEqualTo("EVT-1");
        assertThat(respuesta.nombreAfectado()).isEqualTo("Ana");
        assertThat(respuesta.reportanteId()).isEqualTo("USR-1");
        assertThat(respuesta.ubicacionId()).isEqualTo("EDIF-001");
        assertThat(respuesta.tipoUbicacion()).isEqualTo("EDIFICIO");
        assertThat(respuesta.prioridad()).isEqualTo("EMERGENCIA");
        assertThat(respuesta.estado()).isEqualTo("REPORTADO");
        assertThat(respuesta.descripcion()).isEqualTo("Caída en escaleras");
        assertThat(respuesta.fechaReporte()).isEqualTo(fecha);
    }

    @Test
    void brigadistaBusquedaMapperConvierteLasAsignaciones() {
        AsignacionBrigadista asignacion = new AsignacionBrigadista();
        asignacion.setEventoId("EVT-1");
        asignacion.setUbicacionId("ZONA-001");
        asignacion.setTipoUbicacion(new TipoUbicacion("ZONA_SEGURA", "d"));
        asignacion.setEstado(new EstadoAsignacion("VIGENTE", "d"));

        List<AsignacionBrigadistaResumenResponse> resultado =
                new BrigadistaBusquedaMapper()
                        .convertirAsignaciones(List.of(asignacion));

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).eventoId()).isEqualTo("EVT-1");
        assertThat(resultado.get(0).ubicacionId()).isEqualTo("ZONA-001");
        assertThat(resultado.get(0).tipoUbicacion()).isEqualTo("ZONA_SEGURA");
        assertThat(resultado.get(0).estado()).isEqualTo("VIGENTE");
    }

    @Test
    void brigadistaBusquedaMapperConListaVaciaDevuelveListaVacia() {
        assertThat(new BrigadistaBusquedaMapper()
                .convertirAsignaciones(List.of())).isEmpty();
    }
}