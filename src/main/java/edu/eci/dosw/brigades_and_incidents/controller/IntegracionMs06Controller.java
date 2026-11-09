package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.ActualizarEstadoEventoRequest;
import edu.eci.dosw.brigades_and_incidents.dto.BrigadistasEventoResponse;
import edu.eci.dosw.brigades_and_incidents.dto.IndicadoresEventoResponse;
import edu.eci.dosw.brigades_and_incidents.service.ConsultaBrigadistasEventoService;
import edu.eci.dosw.brigades_and_incidents.service.GestionOperacionEventoService;
import edu.eci.dosw.brigades_and_incidents.service.IndicadoresEventoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bei/eventos")
public class IntegracionMs06Controller {

    private final GestionOperacionEventoService operacionService;
    private final ConsultaBrigadistasEventoService brigadistasService;
    private final IndicadoresEventoService indicadoresService;

    public IntegracionMs06Controller(
            GestionOperacionEventoService operacionService,
            ConsultaBrigadistasEventoService brigadistasService,
            IndicadoresEventoService indicadoresService
    ) {
        this.operacionService = operacionService;
        this.brigadistasService = brigadistasService;
        this.indicadoresService = indicadoresService;
    }

    @PutMapping("/{eventoId}/estado")
    public ResponseEntity<Void> actualizarEstado(
            @PathVariable String eventoId,
            @RequestBody ActualizarEstadoEventoRequest request
    ) {
        operacionService.actualizarOperacion(eventoId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventoId}/brigadistas")
    public ResponseEntity<BrigadistasEventoResponse> consultarBrigadistas(
            @PathVariable String eventoId
    ) {
        return ResponseEntity.ok(
                brigadistasService.consultar(eventoId)
        );
    }

    @GetMapping("/{eventoId}/indicadores")
    public ResponseEntity<IndicadoresEventoResponse> consultarIndicadores(
            @PathVariable String eventoId
    ) {
        return ResponseEntity.ok(
                indicadoresService.consultar(eventoId)
        );
    }
}