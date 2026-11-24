package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.AutoasignacionEventoDTO;
import edu.eci.dosw.brigades_and_incidents.dto.AutoasignacionResponse;
import edu.eci.dosw.brigades_and_incidents.dto.BrigadistaGestionDTO;
import edu.eci.dosw.brigades_and_incidents.dto.EventoOperacionDTO;
import edu.eci.dosw.brigades_and_incidents.dto.GestionBrigadistasEventoResponse;
import edu.eci.dosw.brigades_and_incidents.dto.SeleccionEdificioRequest;
import edu.eci.dosw.brigades_and_incidents.service.AlcanceGestion;
import edu.eci.dosw.brigades_and_incidents.service.OperacionBrigadistasService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Operación de brigadistas en los eventos programados o en ejecución:
 * panel de gestión del coordinador y auto asignación del brigadista.
 */
@RestController
@RequestMapping("/api/v1/brigadistas")
public class OperacionBrigadistasController {

    private final OperacionBrigadistasService service;

    public OperacionBrigadistasController(OperacionBrigadistasService service) {
        this.service = service;
    }

    // Eventos programados o en ejecución que gestiona quien consulta: los reales con assign_to_real y los simulacros
    // con assign_to_drill (permisos de X-User-Permission, nunca el rol).
    @PreAuthorize("hasAnyAuthority('ms05:brigadier:assign_to_drill', 'ms05:brigadier:assign_to_real')")
    @GetMapping("/eventos")
    public ResponseEntity<List<EventoOperacionDTO>> consultarEventos() {
        return ResponseEntity.ok(service.consultarEventosAbiertos(AlcanceGestion.desdePermisos()));
    }

    // Panel de gestión de brigadistas de un evento: coordinador u organizador.
    @PreAuthorize("hasAnyAuthority('ms05:brigadier:assign_to_drill', 'ms05:brigadier:assign_to_real')")
    @GetMapping("/eventos/{eventoId}/gestion")
    public ResponseEntity<GestionBrigadistasEventoResponse> consultarGestion(
            @PathVariable String eventoId
    ) {
        return ResponseEntity.ok(service.consultarGestion(eventoId, AlcanceGestion.desdePermisos()));
    }

    // Cambia el edificio de un brigadista: el coordinador en eventos reales y el organizador en simulacros.
    @PreAuthorize("hasAnyAuthority('ms05:brigadier:assign_to_drill', 'ms05:brigadier:assign_to_real')")
    @PutMapping("/eventos/{eventoId}/brigadistas/{brigadistaId}/asignacion")
    public ResponseEntity<BrigadistaGestionDTO> cambiarAsignacion(
            @PathVariable String eventoId,
            @PathVariable String brigadistaId,
            @Valid @RequestBody SeleccionEdificioRequest request,
            @RequestHeader("X-User-Id") String actorId
    ) {
        return ResponseEntity.ok(service.cambiarAsignacion(
                eventoId, brigadistaId, request.ubicacionId(), actorId, AlcanceGestion.desdePermisos()));
    }

    // Vista de auto asignación del brigadista autenticado.
    @PreAuthorize("hasAuthority('ms05:panel:read_own')")
    @GetMapping("/autoasignacion")
    public ResponseEntity<AutoasignacionResponse> consultarAutoasignacion(
            @RequestHeader("X-User-Id") String brigadistaId
    ) {
        return ResponseEntity.ok(service.consultarAutoasignacion(brigadistaId));
    }

    // El brigadista autenticado elige o cambia su edificio en el evento.
    @PreAuthorize("hasAuthority('ms05:panel:read_own')")
    @PutMapping("/autoasignacion/eventos/{eventoId}")
    public ResponseEntity<AutoasignacionEventoDTO> autoasignarse(
            @PathVariable String eventoId,
            @Valid @RequestBody SeleccionEdificioRequest request,
            @RequestHeader("X-User-Id") String brigadistaId
    ) {
        return ResponseEntity.ok(service.autoasignarse(
                eventoId, brigadistaId, request.ubicacionId()));
    }

    // El brigadista autenticado quita su asignación del evento.
    @PreAuthorize("hasAuthority('ms05:panel:read_own')")
    @DeleteMapping("/autoasignacion/eventos/{eventoId}")
    public ResponseEntity<AutoasignacionEventoDTO> retirarAutoasignacion(
            @PathVariable String eventoId,
            @RequestHeader("X-User-Id") String brigadistaId
    ) {
        return ResponseEntity.ok(service.retirarAutoasignacion(eventoId, brigadistaId));
    }
}
