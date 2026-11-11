
package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.ListadoResponsabilidadesResponse;
import edu.eci.dosw.brigades_and_incidents.dto.PanelResponsabilidadesResponse;
import edu.eci.dosw.brigades_and_incidents.security.Roles;
import edu.eci.dosw.brigades_and_incidents.service.ConsultaResponsabilidadesService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    @PreAuthorize("hasRole('BRIGADISTA')")
    @GetMapping("/mias")
    public ResponseEntity<PanelResponsabilidadesResponse> consultarPanel(
            @RequestHeader("X-User-Id") String brigadistaId,
            @RequestHeader("X-User-Role") String rol,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano
    ) {
        if (!Roles.BRIGADISTA.equalsIgnoreCase(rol)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El panel de responsabilidades está disponible únicamente para brigadistas."
            );
        }

        return ResponseEntity.ok(
                service.consultarPanel(brigadistaId, pagina, tamano)
        );
    }

    // Consultar el listado general de responsabilidades.
    @PreAuthorize("hasAnyRole('COORDINATOR', 'AUDITOR')")
    @GetMapping
    public ResponseEntity<ListadoResponsabilidadesResponse> consultarListadoGeneral(
            @RequestHeader("X-User-Role") String rol,
            @RequestParam(required = false) String eventoId,
            @RequestParam(required = false) String nombreBrigadista,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipoUbicacion,
            @RequestParam(required = false) String ubicacionId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano
    ) {
        validarRolConsultaGeneral(rol);

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

    private void validarRolConsultaGeneral(String rol) {

        boolean autorizado =
                Roles.COORDINATOR.equalsIgnoreCase(rol)
                        || Roles.AUDITOR.equalsIgnoreCase(rol);

        if (!autorizado) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tiene permisos para consultar el listado general de responsabilidades."
            );
        }
    }
}
