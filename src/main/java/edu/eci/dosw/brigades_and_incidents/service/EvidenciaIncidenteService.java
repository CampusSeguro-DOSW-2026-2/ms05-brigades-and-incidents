package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.client.UsuarioClient;
import edu.eci.dosw.brigades_and_incidents.dto.EvidenciaIncidenteResponse;
import edu.eci.dosw.brigades_and_incidents.entity.EvidenciaIncidente;
import edu.eci.dosw.brigades_and_incidents.entity.Incidente;
import edu.eci.dosw.brigades_and_incidents.event.IncidenteAuditPublisher;
import edu.eci.dosw.brigades_and_incidents.exception.EvidenciaIncidenteException;
import edu.eci.dosw.brigades_and_incidents.repository.EvidenciaIncidenteRepository;
import edu.eci.dosw.brigades_and_incidents.repository.IncidenteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EvidenciaIncidenteService {

    private static final int MAXIMO_EVIDENCIAS = 5;

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/png",
            "application/pdf",
            "video/mp4"
    );

    private static final Set<String> ROLES_USUARIO_GENERAL = Set.of(
            "ESTUDIANTE",
            "PROFESOR",
            "ADMINISTRATIVO",
            "COORDINADOR",
            "ORGANIZADOR",
            "ADMIN"
    );

    private static final String ROL_BRIGADISTA = "BRIGADISTA";
    private static final String ROL_AUDITOR = "AUDITOR";

    private final EvidenciaIncidenteRepository evidenciaRepository;
    private final IncidenteRepository incidenteRepository;
    private final EvidenciaStorage evidenciaStorage;
    private final IncidenteAuditPublisher auditPublisher;
    private final UsuarioClient usuarioClient;

    public EvidenciaIncidenteService(
            EvidenciaIncidenteRepository evidenciaRepository,
            IncidenteRepository incidenteRepository,
            EvidenciaStorage evidenciaStorage,
            IncidenteAuditPublisher auditPublisher,
            UsuarioClient usuarioClient
    ) {
        this.evidenciaRepository = evidenciaRepository;
        this.incidenteRepository = incidenteRepository;
        this.evidenciaStorage = evidenciaStorage;
        this.auditPublisher = auditPublisher;
        this.usuarioClient = usuarioClient;
    }

    @Transactional
    public EvidenciaIncidenteResponse cargarEvidencia(
            String eventoId,
            UUID incidenteId,
            MultipartFile archivo,
            String usuarioId
    ) {
        validarPermisos(usuarioId);
        validarArchivo(archivo);

        Incidente incidente = incidenteRepository.findById(incidenteId)
                .orElseThrow(() -> new EvidenciaIncidenteException(
                        "El incidente indicado no existe."
                ));

        if (!incidente.getEventoId().equals(eventoId)) {
            throw new EvidenciaIncidenteException(
                    "El incidente no pertenece al evento indicado."
            );
        }

        if (evidenciaRepository.countByIncidenteId(incidenteId)
                >= MAXIMO_EVIDENCIAS) {
            throw new EvidenciaIncidenteException(
                    "Un incidente no puede tener más de 5 evidencias."
            );
        }

        String referencia = evidenciaStorage.guardar(archivo);

        try {
            EvidenciaIncidente evidencia = new EvidenciaIncidente();

            evidencia.setNombreArchivo(
                    archivo.getOriginalFilename()
            );
            evidencia.setTipoContenido(archivo.getContentType());
            evidencia.setReferenciaArchivo(referencia);
            evidencia.setIncidente(incidente);

            EvidenciaIncidente guardada =
                    evidenciaRepository.save(evidencia);

            auditPublisher.publicar(
                    "CARGAR_EVIDENCIA",
                    "EVIDENCIA_INCIDENTE",
                    guardada.getId().toString(),
                    eventoId,
                    usuarioId,
                    "EXITOSO",
                    "Se cargó una evidencia para el incidente "
                            + incidenteId
            );

            return convertirRespuesta(guardada);

        } catch (RuntimeException e) {
            evidenciaStorage.eliminar(referencia);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<EvidenciaIncidenteResponse> listarEvidencias(
            String eventoId,
            UUID incidenteId
    ) {
        Incidente incidente = incidenteRepository.findById(incidenteId)
                .orElseThrow(() -> new EvidenciaIncidenteException(
                        "El incidente indicado no existe."
                ));

        if (!incidente.getEventoId().equals(eventoId)) {
            throw new EvidenciaIncidenteException(
                    "El incidente no pertenece al evento indicado."
            );
        }

        return evidenciaRepository.findByIncidenteId(incidenteId)
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    private void validarPermisos(String usuarioId) {

        if (usuarioId == null || usuarioId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "No se pudo identificar al usuario autenticado."
            );
        }

        UsuarioClient.UsuarioExterno usuario =
                usuarioClient.consultarUsuario(usuarioId);

        if (usuario == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El usuario no existe en MS01."
            );
        }

        if (usuario.estado() == null
                || !"ACTIVO".equalsIgnoreCase(usuario.estado())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El usuario se encuentra inactivo."
            );
        }

        Set<String> roles = usuario.roles() == null
                ? Set.of()
                : usuario.roles().stream()
                        .filter(rol -> rol != null && !rol.isBlank())
                        .map(rol -> rol.trim().toUpperCase(Locale.ROOT))
                        .collect(Collectors.toSet());

        if (roles.contains(ROL_AUDITOR)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Los usuarios con rol auditor no pueden cargar evidencias."
            );
        }

        boolean usuarioAutorizado = roles.stream()
                .anyMatch(ROLES_USUARIO_GENERAL::contains);

        boolean esBrigadista = roles.contains(ROL_BRIGADISTA);

        if (!usuarioAutorizado && !esBrigadista) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El usuario no tiene un rol autorizado para cargar evidencias."
            );
        }
    }

    private void validarArchivo(MultipartFile archivo) {

        if (archivo == null || archivo.isEmpty()) {
            throw new EvidenciaIncidenteException(
                    "Debe seleccionar un archivo válido."
            );
        }

        String tipoContenido = archivo.getContentType();

        if (tipoContenido == null
                || !TIPOS_PERMITIDOS.contains(tipoContenido)) {
            throw new EvidenciaIncidenteException(
                    "El formato no está permitido. "
                            + "Solo se aceptan PNG, PDF y MP4."
            );
        }

        if (archivo.getOriginalFilename() == null
                || archivo.getOriginalFilename().isBlank()) {
            throw new EvidenciaIncidenteException(
                    "El archivo debe tener un nombre válido."
            );
        }
    }

    private EvidenciaIncidenteResponse convertirRespuesta(
            EvidenciaIncidente evidencia
    ) {
        return new EvidenciaIncidenteResponse(
                evidencia.getId(),
                evidencia.getNombreArchivo(),
                evidencia.getTipoContenido(),
                evidencia.getReferenciaArchivo(),
                evidencia.getFechaCarga()
        );
    }
}