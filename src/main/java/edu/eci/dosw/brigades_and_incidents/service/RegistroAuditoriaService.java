
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.entity.RegistroAuditoria;
import edu.eci.dosw.brigades_and_incidents.entity.TipoRegistroAuditoria;
import edu.eci.dosw.brigades_and_incidents.exception.RecursoNoEncontradoException;
import edu.eci.dosw.brigades_and_incidents.repository.RegistroAuditoriaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.TipoRegistroAuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class RegistroAuditoriaService {

    private final RegistroAuditoriaRepository registroRepository;
    private final TipoRegistroAuditoriaRepository tipoRepository;

    public RegistroAuditoriaService(
            RegistroAuditoriaRepository registroRepository,
            TipoRegistroAuditoriaRepository tipoRepository
    ) {
        this.registroRepository = registroRepository;
        this.tipoRepository = tipoRepository;
    }

    /**
     * Registra una accion realizada dentro del microservicio MS05.
     */
    public RegistroAuditoria registrarAccion(
            String tipoRegistro,
            String usuarioOrigen,
            String accion,
            String recursoTipo,
            String recursoId,
            String eventoId,
            String origen,
            String resultado,
            String detalle
    ) {

        validarCampo(tipoRegistro, "El tipo de registro es obligatorio.");
        validarCampo(usuarioOrigen, "El usuario de origen es obligatorio.");
        validarCampo(accion, "La accion es obligatoria.");
        validarCampo(recursoTipo, "El tipo de recurso es obligatorio.");
        validarCampo(recursoId, "El identificador del recurso es obligatorio.");
        validarCampo(origen, "El origen es obligatorio.");
        validarCampo(resultado, "El resultado es obligatorio.");

        TipoRegistroAuditoria tipo = tipoRepository.findByNombre(
                tipoRegistro.trim().toUpperCase()
        ).orElseThrow(() ->
                new RecursoNoEncontradoException(
                        "No existe el tipo de registro de auditoria: "
                                + tipoRegistro
                )
        );

        RegistroAuditoria registro = new RegistroAuditoria();

        registro.setTipoRegistro(tipo);
        registro.setUsuarioOrigen(usuarioOrigen.trim());
        registro.setAccion(accion.trim());
        registro.setRecursoTipo(recursoTipo.trim());
        registro.setRecursoId(recursoId.trim());
        registro.setEventoId(normalizarOpcional(eventoId));
        registro.setOrigen(origen.trim());
        registro.setResultado(resultado.trim());
        registro.setDetalle(normalizarOpcional(detalle));
        registro.setFechaHoraRegistro(OffsetDateTime.now());

        return registroRepository.save(registro);
    }

    /**
     * Registra una accion conservando la fecha original del evento.
     */
    public RegistroAuditoria registrarAccion(
            String tipoRegistro,
            String usuarioOrigen,
            String accion,
            String recursoTipo,
            String recursoId,
            String eventoId,
            String origen,
            String resultado,
            String detalle,
            OffsetDateTime fechaHoraRegistro
    ) {

        RegistroAuditoria registro = registrarAccion(
                tipoRegistro,
                usuarioOrigen,
                accion,
                recursoTipo,
                recursoId,
                eventoId,
                origen,
                resultado,
                detalle
        );

        if (fechaHoraRegistro != null) {
            registro.setFechaHoraRegistro(fechaHoraRegistro);
            return registroRepository.save(registro);
        }

        return registro;
    }

    /**
     * Consulta los registros de auditoria asociados a un evento.
     */
    @Transactional(readOnly = true)
    public List<RegistroAuditoria> consultarPorEvento(String eventoId) {

        validarCampo(
                eventoId,
                "El identificador del evento es obligatorio."
        );

        return registroRepository
                .findByEventoIdOrderByFechaHoraRegistroAsc(eventoId.trim());
    }

    /**
     * Consulta los registros de auditoria asociados a un recurso.
     */
    @Transactional(readOnly = true)
    public List<RegistroAuditoria> consultarPorRecurso(
            String recursoTipo,
            String recursoId
    ) {

        validarCampo(recursoTipo, "El tipo de recurso es obligatorio.");
        validarCampo(
                recursoId,
                "El identificador del recurso es obligatorio."
        );

        return registroRepository
                .findByRecursoTipoAndRecursoIdOrderByFechaHoraRegistroAsc(
                        recursoTipo.trim(),
                        recursoId.trim()
                );
    }

    /**
     * Consulta todos los registros de auditoria en orden cronologico.
     */
    @Transactional(readOnly = true)
    public List<RegistroAuditoria> consultarTodos() {
        return registroRepository.findAllByOrderByFechaHoraRegistroAsc();
    }

    private void validarCampo(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    private String normalizarOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}
