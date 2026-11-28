
package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.EvidenciaIncidenteResponse;
import edu.eci.dosw.brigades_and_incidents.service.EvidenciaIncidenteService;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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

    // Puede adjuntar quien puede registrar incidentes (ms05:incident:report); el servicio exige ser el dueño del
    // incidente o un brigadista activo.
    @PreAuthorize("hasAuthority('ms05:incident:report')")
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

    @PreAuthorize("hasAuthority('ms05:incident:read')")
    @GetMapping
    public ResponseEntity<List<EvidenciaIncidenteResponse>> listarEvidencias(
            @PathVariable String eventoId,
            @PathVariable UUID incidenteId
    ) {
        return ResponseEntity.ok(
                evidenciaService.listarEvidencias(eventoId, incidenteId)
        );
    }

    // Ver o descargar el archivo de una evidencia. Requiere el mismo permiso de consulta que el listado.
    @PreAuthorize("hasAuthority('ms05:incident:read')")
    @GetMapping("/{evidenciaId}/archivo")
    public ResponseEntity<Resource> verArchivo(
            @PathVariable String eventoId,
            @PathVariable UUID incidenteId,
            @PathVariable UUID evidenciaId
    ) {
        EvidenciaIncidenteService.ArchivoEvidencia archivo =
                evidenciaService.obtenerArchivo(
                        eventoId,
                        incidenteId,
                        evidenciaId
                );

        MediaType tipo;

        try {
            tipo = MediaType.parseMediaType(archivo.tipoContenido());
        } catch (RuntimeException exception) {
            tipo = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(tipo)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(archivo.nombreArchivo())
                                .build()
                                .toString()
                )
                .body(new FileSystemResource(archivo.ruta()));
    }
}
