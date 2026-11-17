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

    private static final String ROLES = "X-User-Role";

    private final OperacionBrigadistasService service;

    public OperacionBrigadistasController(OperacionBrigadistasService service) {
        this.service = service;
    }

    // Eventos programados o en ejecución que gestiona quien consulta: el coordinador los reales y el organizador
    // los simulacros (el rol llega en X-User-Role, que arma el Gateway desde el JWT).
    @PreAuthorize("hasAuthority('ms05:brigadier:assign')")
    @GetMapping("/eventos")
    public ResponseEntity<List<EventoOperacionDTO>> consultarEventos(
            @RequestHeader(value = ROLES, required = false) String roles
    ) {
        return ResponseEntity.ok(service.consultarEventosAbiertos(AlcanceGestion.desdeRoles(roles)));
    }

    // Panel de gestión de brigadistas de un evento: coordinador u organizador.
    @PreAuthorize("hasAuthority('ms05:brigadier:assign')")
    @GetMapping("/eventos/{eventoId}/gestion")
    public ResponseEntity<GestionBrigadistasEventoResponse> consultarGestion(
            @PathVariable String eventoId,
            @RequestHeader(value = ROLES, required = false) String roles
    ) {
        return ResponseEntity.ok(service.consultarGestion(eventoId, AlcanceGestion.desdeRoles(roles)));
    }

    // Cambia el edificio de un brigadista: el coordinador en eventos reales y el organizador en simulacros.
    @PreAuthorize("hasAuthority('ms05:brigadier:assign')")
    @PutMapping("/eventos/{eventoId}/brigadistas/{brigadistaId}/asignacion")
    public ResponseEntity<BrigadistaGestionDTO> cambiarAsignacion(
            @PathVariable String eventoId,
            @PathVariable String brigadistaId,
            @Valid @RequestBody SeleccionEdificioRequest request,
            @RequestHeader("X-User-Id") String actorId,
            @RequestHeader(value = ROLES, required = false) String roles
    ) {
        return ResponseEntity.ok(service.cambiarAsignacion(
                eventoId, brigadistaId, request.ubicacionId(), actorId, AlcanceGestion.desdeRoles(roles)));
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
