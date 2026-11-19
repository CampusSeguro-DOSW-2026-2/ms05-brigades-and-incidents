
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
@RequestMapping("/api/v1/incidentes/eventos/{eventoId}/{incidenteId}")
public class GestionAtencionIncidenteController {

    private final GestionAtencionIncidenteService gestionService;

    public GestionAtencionIncidenteController(
            GestionAtencionIncidenteService gestionService
    ) {
        this.gestionService = gestionService;
    }

    // Asignar un responsable. Coordinadores u organizadores.
    @PreAuthorize("hasAnyAuthority('ms05:assistance:assign_to_drill', 'ms05:assistance:assign_to_real')")
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

    // Iniciar atención. Requiere ms05:assistance:update_status; el servicio exige ser el responsable.
    @PreAuthorize("hasAuthority('ms05:assistance:update_status')")
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

    // Resolver incidente. Requiere ms05:assistance:update_status; el servicio exige ser el responsable.
    @PreAuthorize("hasAuthority('ms05:assistance:update_status')")
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

    // Cerrar incidente. Requiere ms05:assistance:assign_to_drill o assign_to_real según el evento.
    @PreAuthorize("hasAnyAuthority('ms05:assistance:assign_to_drill', 'ms05:assistance:assign_to_real')")
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

    // Consultar historial. Requiere ms05:incident:read.
    @PreAuthorize("hasAuthority('ms05:incident:read')")
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
