
package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.EvidenciaIncidenteResponse;
import edu.eci.dosw.brigades_and_incidents.service.EvidenciaIncidenteService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidentes/eventos/{eventoId}/{incidenteId}/evidencias")
public class EvidenciaIncidenteController {

    private final EvidenciaIncidenteService evidenciaService;

    public EvidenciaIncidenteController(
            EvidenciaIncidenteService evidenciaService
    ) {
        this.evidenciaService = evidenciaService;
    }

    // Puede adjuntar quien puede registrar incidentes (todos menos AUDITOR); el servicio exige ser el dueño del
    // incidente o un brigadista activo.
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR', 'ADMINISTRATIVO', 'BRIGADISTA', 'COORDINADOR', 'ORGANIZADOR', 'ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EvidenciaIncidenteResponse> cargarEvidencia(
            @PathVariable String eventoId,
            @PathVariable UUID incidenteId,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestHeader("X-User-Id") String usuarioId
    ) {
        EvidenciaIncidenteResponse response =
                evidenciaService.cargarEvidencia(
                        eventoId,
                        incidenteId,
                        archivo,
                        usuarioId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PreAuthorize("hasAnyRole('COORDINADOR', 'ORGANIZADOR', 'BRIGADISTA', 'AUDITOR')")
    @GetMapping
    public ResponseEntity<List<EvidenciaIncidenteResponse>> listarEvidencias(
            @PathVariable String eventoId,
            @PathVariable UUID incidenteId
    ) {
        return ResponseEntity.ok(
                evidenciaService.listarEvidencias(eventoId, incidenteId)
        );
    }
}
