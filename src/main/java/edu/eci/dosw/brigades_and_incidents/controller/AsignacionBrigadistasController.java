
package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.AsignacionPorDefectoRequest;
import edu.eci.dosw.brigades_and_incidents.dto.AsignacionResponse;
import edu.eci.dosw.brigades_and_incidents.dto.CrearAsignacionRequest;
import edu.eci.dosw.brigades_and_incidents.dto.ModificarAsignacionRequest;
import edu.eci.dosw.brigades_and_incidents.security.Roles;
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
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ORGANIZADOR')")
    @PostMapping
    public ResponseEntity<AsignacionResponse> crearAsignacion(
            @Valid @RequestBody CrearAsignacionRequest request,
            @RequestHeader("X-User-Id") String actorId,
            @RequestHeader("X-User-Role") String rol) {

        validarRolCoordinador(rol);

        AsignacionResponse respuesta =
                service.crearAsignacion(request, actorId);

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    // El brigadista solicita asignarse a una ubicación.
    @PreAuthorize("hasRole('BRIGADISTA')")
    @PostMapping("/mis-asignaciones")
    public ResponseEntity<AsignacionResponse> autoasignarse(
            @Valid @RequestBody CrearAsignacionRequest request,
            @RequestHeader("X-User-Id") String brigadistaId,
            @RequestHeader("X-User-Role") String rol) {

        if (!Roles.tiene(rol, Roles.BRIGADISTA)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Esta operación está disponible únicamente para brigadistas."
            );
        }

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
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ORGANIZADOR')")
    @PutMapping("/{idAsignacion}")
    public ResponseEntity<AsignacionResponse> modificarAsignacion(
            @PathVariable UUID idAsignacion,
            @Valid @RequestBody ModificarAsignacionRequest request,
            @RequestHeader("X-User-Id") String actorId,
            @RequestHeader("X-User-Role") String rol) {

        validarRolCoordinador(rol);

        return ResponseEntity.ok(
                service.modificarAsignacion(idAsignacion, request, actorId)
        );
    }

    // El coordinador retira una asignación.
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ORGANIZADOR')")
    @PatchMapping("/{idAsignacion}/retiro")
    public ResponseEntity<AsignacionResponse> retirarAsignacion(
            @PathVariable UUID idAsignacion,
            @RequestHeader("X-User-Id") String actorId,
            @RequestHeader("X-User-Role") String rol) {

        validarRolCoordinador(rol);

        return ResponseEntity.ok(
                service.retirarAsignacion(idAsignacion, actorId)
        );
    }

    // MS06 solicita las asignaciones predeterminadas al iniciar un evento.
    @PreAuthorize("hasRole('SERVICE_MS06')")
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
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ORGANIZADOR', 'AUDITOR')")
    @GetMapping("/{idAsignacion}")
    public ResponseEntity<AsignacionResponse> consultarAsignacion(
            @PathVariable UUID idAsignacion) {

        return ResponseEntity.ok(
                service.consultarAsignacion(idAsignacion)
        );
    }

    // Consultar todas las asignaciones de un evento: coordinador o auditor.
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ORGANIZADOR', 'AUDITOR')")
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
            "hasAnyRole('COORDINADOR', 'ORGANIZADOR', 'AUDITOR') "
            + "or (hasRole('BRIGADISTA') and #brigadistaId == authentication.name)"
    )
    @GetMapping("/brigadista/{brigadistaId}")
    public ResponseEntity<List<AsignacionResponse>> consultarPorBrigadista(
            @P("brigadistaId") @PathVariable String brigadistaId) {

        return ResponseEntity.ok(
                service.consultarPorBrigadista(brigadistaId)
        );
    }

    private void validarRolCoordinador(String rol) {

        if (!Roles.tiene(rol, Roles.COORDINADOR) && !Roles.tiene(rol, Roles.ORGANIZADOR)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un coordinador u organizador puede realizar esta operación."
            );
        }
    }
}
