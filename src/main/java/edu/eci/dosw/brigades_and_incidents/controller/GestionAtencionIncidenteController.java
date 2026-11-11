
package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.AsignarResponsableRequest;
import edu.eci.dosw.brigades_and_incidents.dto.CambioEstadoIncidenteResponse;
import edu.eci.dosw.brigades_and_incidents.dto.HistorialIncidenteResponse;
import edu.eci.dosw.brigades_and_incidents.dto.ResolverIncidenteRequest;
import edu.eci.dosw.brigades_and_incidents.service.GestionAtencionIncidenteService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/eventos/{eventoId}/incidentes/{incidenteId}")
public class GestionAtencionIncidenteController {

    private final GestionAtencionIncidenteService gestionService;

    public GestionAtencionIncidenteController(
            GestionAtencionIncidenteService gestionService
    ) {
        this.gestionService = gestionService;
    }

    // Asignar un responsable. Solo coordinadores.
    @PreAuthorize("hasRole('COORDINATOR')")
    @PatchMapping("/responsable")
    public ResponseEntity<CambioEstadoIncidenteResponse> asignarResponsable(
            @PathVariable String eventoId,
            @PathVariable UUID incidenteId,
            @Valid @RequestBody AsignarResponsableRequest request,
            @RequestHeader(value = "X-User-Id", required = false)
            String actorId
    ) {
        return ResponseEntity.ok(
                gestionService.asignarResponsable(
                        eventoId,
                        incidenteId,
                        request,
                        actorId
                )
        );
    }

    // Iniciar atención. Solo brigadistas.
    @PreAuthorize("hasRole('BRIGADISTA')")
    @PostMapping("/iniciar-atencion")
    public ResponseEntity<CambioEstadoIncidenteResponse> iniciarAtencion(
            @PathVariable String eventoId,
            @PathVariable UUID incidenteId,
            @RequestHeader(value = "X-User-Id", required = false)
            String actorId
    ) {
        return ResponseEntity.ok(
                gestionService.iniciarAtencion(
                        eventoId,
                        incidenteId,
                        actorId
                )
        );
    }

    // Resolver incidente. Solo brigadistas.
    @PreAuthorize("hasRole('BRIGADISTA')")
    @PostMapping("/resolver")
    public ResponseEntity<CambioEstadoIncidenteResponse> resolverIncidente(
            @PathVariable String eventoId,
            @PathVariable UUID incidenteId,
            @Valid @RequestBody ResolverIncidenteRequest request,
            @RequestHeader(value = "X-User-Id", required = false)
            String actorId
    ) {
        return ResponseEntity.ok(
                gestionService.resolverIncidente(
                        eventoId,
                        incidenteId,
                        request,
                        actorId
                )
        );
    }

    // Cerrar incidente. Solo coordinadores.
    @PreAuthorize("hasRole('COORDINATOR')")
    @PostMapping("/cerrar")
    public ResponseEntity<CambioEstadoIncidenteResponse> cerrarIncidente(
            @PathVariable String eventoId,
            @PathVariable UUID incidenteId,
            @RequestHeader(value = "X-User-Id", required = false)
            String actorId
    ) {
        return ResponseEntity.ok(
                gestionService.cerrarIncidente(
                        eventoId,
                        incidenteId,
                        actorId
                )
        );
    }

    // Consultar historial. Coordinadores, brigadistas y auditores.
    @PreAuthorize("hasAnyRole('COORDINATOR', 'BRIGADISTA', 'AUDITOR')")
    @GetMapping("/historial")
    public ResponseEntity<List<HistorialIncidenteResponse>> consultarHistorial(
            @PathVariable String eventoId,
            @PathVariable UUID incidenteId,
            @RequestHeader(value = "X-User-Id", required = false)
            String actorId
    ) {
        return ResponseEntity.ok(
                gestionService.consultarHistorial(
                        eventoId,
                        incidenteId,
                        actorId
                )
        );
    }
}
