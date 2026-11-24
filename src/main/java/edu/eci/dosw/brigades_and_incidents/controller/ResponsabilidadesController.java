
package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.ListadoResponsabilidadesResponse;
import edu.eci.dosw.brigades_and_incidents.dto.PanelResponsabilidadesResponse;
import edu.eci.dosw.brigades_and_incidents.service.ConsultaResponsabilidadesService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/responsabilidades")
public class ResponsabilidadesController {

    private final ConsultaResponsabilidadesService service;

    public ResponsabilidadesController(
            ConsultaResponsabilidadesService service
    ) {
        this.service = service;
    }

    // Consultar las responsabilidades propias del brigadista.
    @PreAuthorize("hasAuthority('ms05:panel:read_own')")
    @GetMapping("/mias")
    public ResponseEntity<PanelResponsabilidadesResponse> consultarPanel(
            @RequestHeader("X-User-Id") String brigadistaId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano
    ) {
        return ResponseEntity.ok(
                service.consultarPanel(brigadistaId, pagina, tamano)
        );
    }

    // Consultar el listado general de responsabilidades.
    @PreAuthorize("hasAnyAuthority('ms05:brigadier:assign_to_drill', 'ms05:brigadier:assign_to_real', 'ms05:incident:read')")
    @GetMapping
    public ResponseEntity<ListadoResponsabilidadesResponse> consultarListadoGeneral(
            @RequestParam(required = false) String eventoId,
            @RequestParam(required = false) String nombreBrigadista,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipoUbicacion,
            @RequestParam(required = false) String ubicacionId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano
    ) {
        return ResponseEntity.ok(
                service.consultarListadoGeneral(
                        eventoId,
                        nombreBrigadista,
                        estado,
                        tipoUbicacion,
                        ubicacionId,
                        pagina,
                        tamano
                )
        );
    }
}
