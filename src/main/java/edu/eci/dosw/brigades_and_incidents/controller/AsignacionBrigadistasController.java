
package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.AsignacionPorDefectoRequest;
import edu.eci.dosw.brigades_and_incidents.dto.AsignacionResponse;
import edu.eci.dosw.brigades_and_incidents.dto.CrearAsignacionRequest;
import edu.eci.dosw.brigades_and_incidents.dto.ModificarAsignacionRequest;
import edu.eci.dosw.brigades_and_incidents.service.AsignacionBrigadistasService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // Coordinador u organizador crea una asignación.
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
    // El ID del brigadista se obtiene del usuario autenticado,
    // no del cuerpo de la solicitud.
    @PostMapping("/mis-asignaciones")
    public ResponseEntity<AsignacionResponse> autoasignarse(
            @Valid @RequestBody CrearAsignacionRequest request,
            @RequestHeader("X-User-Id") String brigadistaId,
            @RequestHeader("X-User-Role") String rol) {

        if (!"BRIGADISTA".equalsIgnoreCase(rol)) {
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

    // Coordinador u organizador modifica una asignación.
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

    // Coordinador u organizador retira una asignación.
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

    // MS06 solicita asignaciones predeterminadas al iniciar un evento.
    @PostMapping("/predeterminadas")
    public ResponseEntity<List<AsignacionResponse>> crearPorDefecto(
            @Valid @RequestBody List<@Valid AsignacionPorDefectoRequest> solicitudes,
            @RequestHeader("X-User-Id") String actorId,
            @RequestHeader("X-User-Role") String rol) {

        if (!"SERVICE_MS06".equalsIgnoreCase(rol)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Esta operación está reservada para el servicio autorizado de MS06."
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(
                service.crearAsignacionesPorDefecto(solicitudes, actorId)
        );
    }

    // Consultar una asignación específica.
    @GetMapping("/{idAsignacion}")
    public ResponseEntity<AsignacionResponse> consultarAsignacion(
            @PathVariable UUID idAsignacion) {

        return ResponseEntity.ok(
                service.consultarAsignacion(idAsignacion)
        );
    }

    // Consultar las asignaciones de un evento.
    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<List<AsignacionResponse>> consultarPorEvento(
            @PathVariable String eventoId) {

        return ResponseEntity.ok(
                service.consultarPorEvento(eventoId)
        );
    }

    // Consultar las asignaciones de un brigadista.
    @GetMapping("/brigadista/{brigadistaId}")
    public ResponseEntity<List<AsignacionResponse>> consultarPorBrigadista(
            @PathVariable String brigadistaId) {

        return ResponseEntity.ok(
                service.consultarPorBrigadista(brigadistaId)
        );
    }

    private void validarRolCoordinador(String rol) {

        boolean autorizado =
                "COORDINATOR".equalsIgnoreCase(rol)
                || "ORGANIZER".equalsIgnoreCase(rol);

        if (!autorizado) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un coordinador u organizador puede realizar esta operación."
            );
        }
    }
}
