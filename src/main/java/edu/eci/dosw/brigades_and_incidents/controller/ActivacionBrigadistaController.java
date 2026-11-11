
package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.ActivacionBrigadistaResponse;
import edu.eci.dosw.brigades_and_incidents.dto.CambiarEstadoBrigadistaRequest;
import edu.eci.dosw.brigades_and_incidents.service.ActivarBrigadistaService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/brigadistas")
public class ActivacionBrigadistaController {

    private final ActivarBrigadistaService activarBrigadistaService;

    public ActivacionBrigadistaController(
            ActivarBrigadistaService activarBrigadistaService
    ) {
        this.activarBrigadistaService = activarBrigadistaService;
    }

    // Cambiar el estado de un brigadista: coordinadores y organizadores, o el propio brigadista
    // (activarse o quedar no disponible).
    @PreAuthorize(
            "hasAnyRole('COORDINADOR', 'ORGANIZADOR') "
            + "or (hasRole('BRIGADISTA') and #brigadistaId == authentication.name)"
    )
    @PutMapping("/{brigadistaId}/estado")
    public ResponseEntity<ActivacionBrigadistaResponse> cambiarEstado(
            @P("brigadistaId") @PathVariable String brigadistaId,
            @Valid @RequestBody CambiarEstadoBrigadistaRequest request
    ) {

        ActivacionBrigadistaResponse response =
                activarBrigadistaService.cambiarEstado(
                        brigadistaId,
                        request
                );

        return ResponseEntity.ok(response);
    }
}
