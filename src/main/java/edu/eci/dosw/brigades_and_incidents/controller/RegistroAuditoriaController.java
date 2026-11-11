
package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.RegistroAuditoriaResponse;
import edu.eci.dosw.brigades_and_incidents.entity.RegistroAuditoria;
import edu.eci.dosw.brigades_and_incidents.service.RegistroAuditoriaService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bei/auditoria")
public class RegistroAuditoriaController {

    private final RegistroAuditoriaService auditoriaService;

    public RegistroAuditoriaController(
            RegistroAuditoriaService auditoriaService
    ) {
        this.auditoriaService = auditoriaService;
    }

    /**
     * Consulta los registros de auditoria asociados a un evento.
     * Solo los auditores pueden realizar esta consulta.
     */
    @PreAuthorize("hasRole('AUDITOR')")
    @GetMapping("/eventos/{eventoId}")
    public List<RegistroAuditoriaResponse> consultarPorEvento(
            @PathVariable String eventoId
    ) {
        return auditoriaService.consultarPorEvento(eventoId)
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    /**
     * Consulta los registros de auditoria asociados a un recurso.
     * Solo los auditores pueden realizar esta consulta.
     */
    @PreAuthorize("hasRole('AUDITOR')")
    @GetMapping("/recursos/{recursoTipo}/{recursoId}")
    public List<RegistroAuditoriaResponse> consultarPorRecurso(
            @PathVariable String recursoTipo,
            @PathVariable String recursoId
    ) {
        return auditoriaService.consultarPorRecurso(
                        recursoTipo,
                        recursoId
                )
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    /**
     * Consulta todos los registros de auditoria.
     * Solo los auditores pueden realizar esta consulta.
     */
    @PreAuthorize("hasRole('AUDITOR')")
    @GetMapping
    public List<RegistroAuditoriaResponse> consultarTodos() {
        return auditoriaService.consultarTodos()
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    private RegistroAuditoriaResponse convertirRespuesta(
            RegistroAuditoria registro
    ) {
        return new RegistroAuditoriaResponse(
                registro.getId(),
                registro.getTipoRegistro().getNombre(),
                registro.getUsuarioOrigen(),
                registro.getAccion(),
                registro.getRecursoTipo(),
                registro.getRecursoId(),
                registro.getFechaHoraRegistro(),
                registro.getEventoId(),
                registro.getOrigen(),
                registro.getResultado(),
                registro.getDetalle()
        );
    }
}
