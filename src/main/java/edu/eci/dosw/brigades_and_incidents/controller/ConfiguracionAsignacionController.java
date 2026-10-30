
package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.entity.ConfiguracionAsignacion;
import edu.eci.dosw.brigades_and_incidents.enums.TipoUbicacion;
import edu.eci.dosw.brigades_and_incidents.service.ConfiguracionAsignacionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/configuraciones-asignacion")
public class ConfiguracionAsignacionController {

    private final ConfiguracionAsignacionService configuracionService;

    public ConfiguracionAsignacionController(
            ConfiguracionAsignacionService configuracionService) {
        this.configuracionService = configuracionService;
    }

    // Registrar una configuración global.
    @PostMapping
    public ResponseEntity<ConfiguracionAsignacion> registrar(
            @RequestBody ConfiguracionAsignacion configuracion) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configuracionService.registrar(configuracion));
    }

    // Consultar todas las configuraciones.
    @GetMapping
    public ResponseEntity<List<ConfiguracionAsignacion>> listarTodas() {
        return ResponseEntity.ok(configuracionService.listarTodas());
    }

    // Consultar una configuración por tipo de ubicación.
    @GetMapping("/tipo/{tipoUbicacion}")
    public ResponseEntity<ConfiguracionAsignacion> consultarPorTipo(
            @PathVariable TipoUbicacion tipoUbicacion) {

        return ResponseEntity.ok(
                configuracionService.consultarPorTipoUbicacion(tipoUbicacion));
    }

    // Consultar una configuración por su identificador.
    @GetMapping("/{id}")
    public ResponseEntity<ConfiguracionAsignacion> consultarPorId(
            @PathVariable UUID id) {

        return ResponseEntity.ok(configuracionService.consultarPorId(id));
    }
}
