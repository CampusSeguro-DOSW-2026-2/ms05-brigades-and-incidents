package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.ActivacionBrigadistaResponse;
import edu.eci.dosw.brigades_and_incidents.dto.CambiarEstadoBrigadistaRequest;
import edu.eci.dosw.brigades_and_incidents.service.ActivarBrigadistaService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
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

    @PutMapping("/{brigadistaId}/estado")
    public ResponseEntity<ActivacionBrigadistaResponse> cambiarEstado(
            @PathVariable String brigadistaId,
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