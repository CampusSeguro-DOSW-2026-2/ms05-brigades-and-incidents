
package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.AsignacionPredeterminadaRequest;
import edu.eci.dosw.brigades_and_incidents.dto.AsignacionPredeterminadaResponse;
import edu.eci.dosw.brigades_and_incidents.dto.ConfirmarAsignacionesRequest;
import edu.eci.dosw.brigades_and_incidents.dto.HistorialAsignacionDTO;
import edu.eci.dosw.brigades_and_incidents.dto.ModificarAsignacionRequest;

import edu.eci.dosw.brigades_and_incidents.entity.AsignacionBrigadistas;
import edu.eci.dosw.brigades_and_incidents.entity.Brigadista;

import edu.eci.dosw.brigades_and_incidents.enums.EstadoAsignacion;

import edu.eci.dosw.brigades_and_incidents.service.AsignacionBrigadistasService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/asignaciones-brigadistas")
public class AsignacionBrigadistasController {

    private final AsignacionBrigadistasService asignacionBrigadistasService;

    public AsignacionBrigadistasController(
            AsignacionBrigadistasService asignacionBrigadistasService) {
        this.asignacionBrigadistasService = asignacionBrigadistasService;
    }

    // Registrar una asignación individual.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AsignacionBrigadistas registrar(
            @RequestBody AsignacionBrigadistas asignacion) {

        return asignacionBrigadistasService.registrar(asignacion);
    }

    // Consultar brigadistas activos sin asignaciones vigentes.
    @GetMapping("/brigadistas/disponibles")
    public List<Brigadista> listarBrigadistasActivos() {

        return asignacionBrigadistasService.listarBrigadistasActivos();
    }

    // MS06 envía el evento y las ubicaciones para generar la propuesta.
    @PostMapping("/evento/{eventoId}/propuesta")
    public AsignacionPredeterminadaResponse prepararAsignacionesPredeterminadas(
            @PathVariable UUID eventoId,
            @RequestBody AsignacionPredeterminadaRequest solicitud) {

        if (solicitud == null
                || solicitud.getEventoId() == null
                || !eventoId.equals(solicitud.getEventoId())) {
            throw new IllegalArgumentException(
                    "El ID del evento no coincide con la solicitud");
        }

        return asignacionBrigadistasService
                .prepararPropuestaConResumen(solicitud);
    }

    // MS06 envía la distribución definitiva después de que el organizador la confirma.
    @PostMapping("/evento/{eventoId}/confirmar")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AsignacionBrigadistas> confirmarAsignaciones(
            @PathVariable UUID eventoId,
            @RequestBody ConfirmarAsignacionesRequest solicitud) {

        return asignacionBrigadistasService
                .confirmarAsignaciones(eventoId, solicitud);
    }

    // Consultar las asignaciones de un evento.
    @GetMapping("/evento/{eventoId}")
    public List<AsignacionBrigadistas> listarPorEvento(
            @PathVariable UUID eventoId) {

        return asignacionBrigadistasService.listarPorEvento(eventoId);
    }

    // Consultar las asignaciones de un brigadista.
    @GetMapping("/brigadista/{brigadistaId}")
    public List<AsignacionBrigadistas> listarPorBrigadista(
            @PathVariable UUID brigadistaId) {

        return asignacionBrigadistasService.listarPorBrigadista(brigadistaId);
    }

    // Consultar el historial de cambios de ubicación de una asignación.
    @GetMapping("/{asignacionId}/historial")
    public List<HistorialAsignacionDTO> consultarHistorial(
            @PathVariable UUID asignacionId) {

        return asignacionBrigadistasService.consultarHistorial(asignacionId);
    }

    // Retirar una asignación.
    @PatchMapping("/{id}/retirar")
    public AsignacionBrigadistas retirar(@PathVariable UUID id) {

        return asignacionBrigadistasService.retirar(id);
    }

    // Modificar la ubicación de una asignación.
    @PutMapping("/{id}")
    public AsignacionBrigadistas modificar(
            @PathVariable UUID id,
            @RequestBody ModificarAsignacionRequest solicitud) {

        if (solicitud == null) {
            throw new IllegalArgumentException(
                    "La información para modificar la asignación es obligatoria");
        }

        return asignacionBrigadistasService.modificar(
                id,
                solicitud.getNuevaUbicacionId(),
                solicitud.getNuevoTipoUbicacion(),
                solicitud.getNuevasObservaciones()
        );
    }

    // Consultar asignaciones por estado.
    @GetMapping("/estado/{estado}")
    public List<AsignacionBrigadistas> listarPorEstado(
            @PathVariable EstadoAsignacion estado) {

        return asignacionBrigadistasService.listarPorEstado(estado);
    }
}
