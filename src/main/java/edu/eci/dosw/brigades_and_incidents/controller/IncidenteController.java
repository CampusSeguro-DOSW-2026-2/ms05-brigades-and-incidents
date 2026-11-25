
package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.ConsultaIncidentesRequest;
import edu.eci.dosw.brigades_and_incidents.dto.IncidenteDetalleDTO;
import edu.eci.dosw.brigades_and_incidents.dto.IncidenteResponse;
import edu.eci.dosw.brigades_and_incidents.dto.IncidenteResumenDTO;
import edu.eci.dosw.brigades_and_incidents.dto.PaginaResponse;
import edu.eci.dosw.brigades_and_incidents.dto.CrearIncidenteRequest;
import edu.eci.dosw.brigades_and_incidents.dto.EditarIncidenteRequest;
import edu.eci.dosw.brigades_and_incidents.service.ConsultaIncidentesService;
import edu.eci.dosw.brigades_and_incidents.service.IncidenteService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidentes/eventos/{eventoId}")
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Los servicios son dependencias inyectadas por Spring y se conservan como referencias finales para su uso por el controlador."
)
public class IncidenteController {

    private final IncidenteService incidenteService;
    private final ConsultaIncidentesService consultaIncidentesService;

    public IncidenteController(
            IncidenteService incidenteService,
            ConsultaIncidentesService consultaIncidentesService
    ) {
        this.incidenteService = incidenteService;
        this.consultaIncidentesService = consultaIncidentesService;
    }

    // Registrar un incidente. Requiere ms05:incident:report (todos menos el auditor).
    @PreAuthorize("hasAuthority('ms05:incident:report')")
    @PostMapping
    public ResponseEntity<IncidenteResponse> registrarIncidente(
            @PathVariable String eventoId,
            @Valid @RequestBody CrearIncidenteRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String reportanteId
    ) {

        IncidenteResponse response =
                incidenteService.registrarIncidente(eventoId, request, reportanteId);

        return ResponseEntity.ok(response);
    }

    // Modificar un incidente (descripción o prioridad). El servicio exige ser el dueño o un brigadista activo.
    @PreAuthorize("hasAnyAuthority('ms05:incident:update', 'ms05:incident:report')")
    @PutMapping("/{incidenteId}")
    public ResponseEntity<IncidenteResponse> editarIncidente(
            @PathVariable String eventoId,
            @PathVariable UUID incidenteId,
            @Valid @RequestBody EditarIncidenteRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String usuarioId
    ) {

        IncidenteResponse response =
                incidenteService.editarIncidente(
                        eventoId,
                        incidenteId,
                        request,
                        usuarioId
                );

        return ResponseEntity.ok(response);
    }

    // Consultar incidentes. Requiere ms05:incident:read; con soloPropios=true basta ms05:incident:manage_own y
    // devuelve solo los que reportó el usuario.
    @PreAuthorize("hasAnyAuthority('ms05:incident:read', 'ms05:incident:manage_own')")
    @GetMapping("/consulta")
    public ResponseEntity<PaginaResponse<IncidenteResumenDTO>> consultarIncidentes(
            @PathVariable String eventoId,
            @RequestParam(required = false) String prioridad,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String ubicacionId,
            @RequestParam(required = false) String ordenarPor,
            @RequestParam(required = false) String direccion,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanio,
            @RequestParam(required = false, defaultValue = "false") boolean soloPropios,
            @RequestHeader(value = "X-User-Id", required = false) String usuarioId
    ) {
        // Paginación de los Estándares (§9): page y size; pagina y tamanio se aceptan por compatibilidad.
        Integer numeroPagina = page != null ? page : pagina;
        Integer tamanioPagina = size != null ? size : tamanio;

        ConsultaIncidentesRequest filtros = new ConsultaIncidentesRequest(
                prioridad,
                estado,
                ubicacionId,
                ordenarPor,
                direccion,
                numeroPagina,
                tamanioPagina
        );

        Page<IncidenteResumenDTO> resultado =
                consultaIncidentesService.consultarIncidentes(
                        eventoId,
                        filtros,
                        usuarioId,
                        soloPropios
                );

        return ResponseEntity.ok(PaginaResponse.de(resultado));
    }

    // Consultar el detalle de un incidente. Requiere ms05:incident:read.
    @PreAuthorize("hasAuthority('ms05:incident:read')")
    @GetMapping("/{incidenteId}")
    public ResponseEntity<IncidenteDetalleDTO> consultarDetalleIncidente(
            @PathVariable String eventoId,
            @PathVariable UUID incidenteId,
            @RequestHeader(value = "X-User-Id", required = false) String usuarioId
    ) {

        IncidenteDetalleDTO resultado =
                consultaIncidentesService.consultarDetalleIncidente(
                        eventoId,
                        incidenteId,
                        usuarioId
                );

        return ResponseEntity.ok(resultado);
    }

    /**
     * Elimina logicamente un incidente resuelto.
     * Requiere ms05:assistance:assign_to_drill o assign_to_real según el evento.
     */
    @PreAuthorize("hasAnyAuthority('ms05:assistance:assign_to_drill', 'ms05:assistance:assign_to_real')")
    @DeleteMapping("/{incidenteId}")
    public ResponseEntity<Void> eliminarIncidente(
            @PathVariable String eventoId,
            @PathVariable UUID incidenteId,
            @RequestHeader(value = "X-User-Id", required = false) String usuarioId
    ) {

        consultaIncidentesService.eliminarIncidente(
                eventoId,
                incidenteId,
                usuarioId
        );

        return ResponseEntity.noContent().build();
    }
}
