package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.ListadoResponsabilidadesResponse;
import edu.eci.dosw.brigades_and_incidents.dto.PanelResponsabilidadesResponse;
import edu.eci.dosw.brigades_and_incidents.service.ConsultaResponsabilidadesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/mias")
    public ResponseEntity<PanelResponsabilidadesResponse> consultarPanel(
            @RequestHeader("X-User-Id") String brigadistaId,
            @RequestHeader("X-User-Role") String rol,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano
    ) {
        if (!"BRIGADISTA".equalsIgnoreCase(rol)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El panel de responsabilidades está disponible únicamente para brigadistas."
            );
        }

        return ResponseEntity.ok(
                service.consultarPanel(brigadistaId, pagina, tamano)
        );
    }

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
                "COORDINATOR".equalsIgnoreCase(rol)
                        || "ORGANIZER".equalsIgnoreCase(rol)
                        || "AUDITOR".equalsIgnoreCase(rol);

        if (!autorizado) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tiene permisos para consultar el listado general de responsabilidades."
            );
        }
    }
}
