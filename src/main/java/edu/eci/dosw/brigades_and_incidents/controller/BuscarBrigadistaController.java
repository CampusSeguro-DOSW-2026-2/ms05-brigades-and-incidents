
package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.BusquedaBrigadistasResponse;
import edu.eci.dosw.brigades_and_incidents.dto.CambiarEstadoBrigadistaRequest;
import edu.eci.dosw.brigades_and_incidents.security.Roles;
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

    @PreAuthorize("hasRole('COORDINATOR')")
    @GetMapping("/buscar")
    public BusquedaBrigadistasResponse buscarBrigadistas(
            @RequestHeader("X-User-Id") String usuarioId,
            @RequestHeader("X-User-Role") String rolUsuario,
            @RequestParam String nombreBrigadista
    ) {
        validarCoordinador(usuarioId, rolUsuario);

        return buscarBrigadistaService.buscarBrigadistas(
                nombreBrigadista
        );
    }

    @PreAuthorize("hasRole('COORDINATOR')")
    @PatchMapping("/{brigadistaId}/estado")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarEstado(
            @PathVariable String brigadistaId,
            @RequestHeader("X-User-Id") String usuarioId,
            @RequestHeader("X-User-Role") String rolUsuario,
            @RequestBody CambiarEstadoBrigadistaRequest request
    ) {
        validarCoordinador(usuarioId, rolUsuario);

        buscarBrigadistaService.cambiarEstado(
                brigadistaId,
                request.estado()
        );
    }

    private void validarCoordinador(
            String usuarioId,
            String rolUsuario
    ) {
        if (usuarioId == null || usuarioId.isBlank()
                || rolUsuario == null || rolUsuario.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "El usuario no está autenticado"
            );
        }

        if (!Roles.COORDINATOR.equalsIgnoreCase(rolUsuario.trim())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tiene permisos para realizar esta operación"
            );
        }
    }
}
