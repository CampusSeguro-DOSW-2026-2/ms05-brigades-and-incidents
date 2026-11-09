package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.BusquedaBrigadistasResponse;
import edu.eci.dosw.brigades_and_incidents.dto.CambiarEstadoBrigadistaRequest;
import edu.eci.dosw.brigades_and_incidents.service.BuscarBrigadistaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/brigadistas")
public class BuscarBrigadistaController {

    private static final Set<String> ROLES_AUTORIZADOS = Set.of(
            "COORDINATOR",
            "ORGANIZER",
            "EVENT_ORGANIZER",
            "COORDINADOR_EMERGENCIA",
            "ORGANIZADOR"
    );

    private final BuscarBrigadistaService buscarBrigadistaService;

    public BuscarBrigadistaController(
            BuscarBrigadistaService buscarBrigadistaService
    ) {
        this.buscarBrigadistaService = buscarBrigadistaService;
    }

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

        String rolNormalizado = rolUsuario
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!ROLES_AUTORIZADOS.contains(rolNormalizado)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tiene permisos para realizar esta operación"
            );
        }
    }
}