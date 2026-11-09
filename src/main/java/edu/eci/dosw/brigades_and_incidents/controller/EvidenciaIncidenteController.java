package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.EvidenciaIncidenteResponse;
import edu.eci.dosw.brigades_and_incidents.service.EvidenciaIncidenteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/eventos/{eventoId}/incidentes/{incidenteId}/evidencias"
)
public class EvidenciaIncidenteController {

    private final EvidenciaIncidenteService evidenciaService;

    public EvidenciaIncidenteController(
            EvidenciaIncidenteService evidenciaService
    ) {
        this.evidenciaService = evidenciaService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EvidenciaIncidenteResponse> cargarEvidencia(
            @PathVariable String eventoId,
            @PathVariable UUID incidenteId,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestHeader(value = "X-User-Id", required = false)
            String usuarioId
    ) {
        EvidenciaIncidenteResponse respuesta =
                evidenciaService.cargarEvidencia(
                        eventoId,
                        incidenteId,
                        archivo,
                        usuarioId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<EvidenciaIncidenteResponse>> listarEvidencias(
            @PathVariable String eventoId,
            @PathVariable UUID incidenteId
    ) {
        List<EvidenciaIncidenteResponse> evidencias =
                evidenciaService.listarEvidencias(
                        eventoId,
                        incidenteId
                );

        return ResponseEntity.ok(evidencias);
    }
}