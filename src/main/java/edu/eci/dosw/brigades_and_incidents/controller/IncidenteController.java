
package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.ConsultaIncidentesRequest;
import edu.eci.dosw.brigades_and_incidents.dto.IncidenteDetalleDTO;
import edu.eci.dosw.brigades_and_incidents.dto.IncidenteResponse;
import edu.eci.dosw.brigades_and_incidents.dto.IncidenteResumenDTO;
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

    // Registrar un incidente. Requiere un usuario autenticado.
    @PreAuthorize("isAuthenticated()")
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

    // Modificar un incidente. Solo coordinadores.
    @PreAuthorize("hasRole('COORDINADOR')")
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

    // Consultar incidentes. Requiere un usuario autenticado.
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/consulta")
    public ResponseEntity<Page<IncidenteResumenDTO>> consultarIncidentes(
            @PathVariable String eventoId,
            @RequestParam(required = false) String prioridad,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String ubicacionId,
            @RequestParam(required = false) String ordenarPor,
            @RequestParam(required = false) String direccion,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanio,
            @RequestHeader(value = "X-User-Id", required = false) String usuarioId
    ) {

        ConsultaIncidentesRequest filtros = new ConsultaIncidentesRequest(
                prioridad,
                estado,
                ubicacionId,
                ordenarPor,
                direccion,
                pagina,
                tamanio
        );

        Page<IncidenteResumenDTO> resultado =
                consultaIncidentesService.consultarIncidentes(
                        eventoId,
                        filtros,
                        usuarioId
                );

        return ResponseEntity.ok(resultado);
    }

    // Consultar el detalle de un incidente. Requiere autenticación.
    @PreAuthorize("isAuthenticated()")
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
     * Solo los coordinadores pueden realizar esta operación.
     */
    @PreAuthorize("hasRole('COORDINADOR')")
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
