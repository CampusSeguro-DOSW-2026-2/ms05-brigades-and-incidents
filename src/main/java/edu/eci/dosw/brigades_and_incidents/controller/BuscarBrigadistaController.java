package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.BusquedaBrigadistasResponse;
import edu.eci.dosw.brigades_and_incidents.dto.CambiarEstadoBrigadistaRequest;
import edu.eci.dosw.brigades_and_incidents.service.BuscarBrigadistaService;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/brigadistas")
public class BuscarBrigadistaController {

    private final BuscarBrigadistaService buscarBrigadistaService;

    public BuscarBrigadistaController(
            BuscarBrigadistaService buscarBrigadistaService
    ) {
        this.buscarBrigadistaService = buscarBrigadistaService;
    }

    @PreAuthorize("hasAuthority('ms05:brigadier:assign')")
    @GetMapping("/buscar")
    public BusquedaBrigadistasResponse buscarBrigadistas(
            @RequestHeader("X-User-Id") String usuarioId,
            // Sin nombre (o vacío) devuelve todos los brigadistas, activos o no
            @RequestParam(required = false) String nombreBrigadista
    ) {
        validarIdentidad(usuarioId);

        return buscarBrigadistaService.buscarBrigadistas(
                nombreBrigadista
        );
    }

    @PreAuthorize("hasAuthority('ms05:brigadier:assign')")
    @PatchMapping("/{brigadistaId}/estado")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarEstado(
            @PathVariable String brigadistaId,
            @RequestHeader("X-User-Id") String usuarioId,
            @RequestBody CambiarEstadoBrigadistaRequest request
    ) {
        validarIdentidad(usuarioId);

        buscarBrigadistaService.cambiarEstado(
                brigadistaId,
                request.estado()
        );
    }

    private void validarIdentidad(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "El usuario no está autenticado"
            );
        }
    }
}
