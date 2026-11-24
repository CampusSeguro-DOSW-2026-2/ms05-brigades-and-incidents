
package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.AsignacionPorDefectoRequest;
import edu.eci.dosw.brigades_and_incidents.dto.AsignacionResponse;
import edu.eci.dosw.brigades_and_incidents.dto.CrearAsignacionRequest;
import edu.eci.dosw.brigades_and_incidents.dto.ModificarAsignacionRequest;
import edu.eci.dosw.brigades_and_incidents.service.AsignacionBrigadistasService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/asignaciones")
public class AsignacionBrigadistasController {

    private final AsignacionBrigadistasService service;

    public AsignacionBrigadistasController(
            AsignacionBrigadistasService service) {
        this.service = service;
    }

    // El coordinador crea una asignación.
    @PreAuthorize("hasAnyAuthority('ms05:brigadier:assign_to_drill', 'ms05:brigadier:assign_to_real')")
    @PostMapping
    public ResponseEntity<AsignacionResponse> crearAsignacion(
            @Valid @RequestBody CrearAsignacionRequest request,
            @RequestHeader("X-User-Id") String actorId) {

        AsignacionResponse respuesta =
                service.crearAsignacion(request, actorId);

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    // El brigadista solicita asignarse a una ubicación.
    @PreAuthorize("hasAuthority('ms05:panel:read_own')")
    @PostMapping("/mis-asignaciones")
    public ResponseEntity<AsignacionResponse> autoasignarse(
            @Valid @RequestBody CrearAsignacionRequest request,
            @RequestHeader("X-User-Id") String brigadistaId) {

        CrearAsignacionRequest solicitud = new CrearAsignacionRequest(
                brigadistaId,
                request.eventoId(),
                request.ubicacionId(),
                request.tipoUbicacion(),
                request.observaciones()
        );

        AsignacionResponse respuesta =
                service.crearAsignacion(solicitud, brigadistaId);

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    // El coordinador modifica una asignación.
    @PreAuthorize("hasAnyAuthority('ms05:brigadier:assign_to_drill', 'ms05:brigadier:assign_to_real')")
    @PutMapping("/{idAsignacion}")
    public ResponseEntity<AsignacionResponse> modificarAsignacion(
            @PathVariable UUID idAsignacion,
            @Valid @RequestBody ModificarAsignacionRequest request,
            @RequestHeader("X-User-Id") String actorId) {

        return ResponseEntity.ok(
                service.modificarAsignacion(idAsignacion, request, actorId)
        );
    }

    // El coordinador retira una asignación.
    @PreAuthorize("hasAnyAuthority('ms05:brigadier:assign_to_drill', 'ms05:brigadier:assign_to_real')")
    @PatchMapping("/{idAsignacion}/retiro")
    public ResponseEntity<AsignacionResponse> retirarAsignacion(
            @PathVariable UUID idAsignacion,
            @RequestHeader("X-User-Id") String actorId) {

        return ResponseEntity.ok(
                service.retirarAsignacion(idAsignacion, actorId)
        );
    }

    // MS06 solicita las asignaciones predeterminadas al iniciar un evento.
    @PreAuthorize("hasAuthority('SERVICE_MS06')")
    @PostMapping("/predeterminadas")
    public ResponseEntity<List<AsignacionResponse>> crearPorDefecto(
            @Valid @RequestBody List<@Valid AsignacionPorDefectoRequest> solicitudes,
            @RequestHeader(value = "X-User-Id", required = false) String actorId) {

        // Solo MS06: la autoridad SERVICE_MS06 sale de X-Service-Name, que el Gateway deriva de su clave.
        // El actor es el usuario por el que actúa MS06, si lo propaga; si no, el propio servicio.
        String actor = actorId != null && !actorId.isBlank() ? actorId : "ms06";
        return ResponseEntity.status(HttpStatus.CREATED).body(
                service.crearAsignacionesPorDefecto(solicitudes, actor)
        );
    }

    // Consultar una asignación específica: coordinador o auditor.
    @PreAuthorize("hasAnyAuthority('ms05:brigadier:assign_to_drill', 'ms05:brigadier:assign_to_real', 'ms05:incident:read')")
    @GetMapping("/{idAsignacion}")
    public ResponseEntity<AsignacionResponse> consultarAsignacion(
            @PathVariable UUID idAsignacion) {

        return ResponseEntity.ok(
                service.consultarAsignacion(idAsignacion)
        );
    }

    // Consultar todas las asignaciones de un evento: coordinador o auditor.
    @PreAuthorize("hasAnyAuthority('ms05:brigadier:assign_to_drill', 'ms05:brigadier:assign_to_real', 'ms05:incident:read')")
    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<List<AsignacionResponse>> consultarPorEvento(
            @PathVariable String eventoId) {

        return ResponseEntity.ok(
                service.consultarPorEvento(eventoId)
        );
    }

    // Un brigadista solo puede consultar sus propias asignaciones.
    // Coordinador y auditor pueden consultar las de cualquier brigadista.
    @PreAuthorize(
            "hasAnyAuthority('ms05:brigadier:assign_to_drill', 'ms05:brigadier:assign_to_real', 'ms05:incident:read') "
            + "or (hasAuthority('ms05:panel:read_own') and #brigadistaId == authentication.name)"
    )
    @GetMapping("/brigadista/{brigadistaId}")
    public ResponseEntity<List<AsignacionResponse>> consultarPorBrigadista(
            @P("brigadistaId") @PathVariable String brigadistaId) {

        return ResponseEntity.ok(
                service.consultarPorBrigadista(brigadistaId)
        );
    }
}
