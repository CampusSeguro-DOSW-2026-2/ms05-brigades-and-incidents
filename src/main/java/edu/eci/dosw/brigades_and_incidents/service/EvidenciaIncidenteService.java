
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.client.UsuarioClient;
import edu.eci.dosw.brigades_and_incidents.dto.EvidenciaIncidenteResponse;
import edu.eci.dosw.brigades_and_incidents.entity.Brigadista;
import edu.eci.dosw.brigades_and_incidents.entity.EvidenciaIncidente;
import edu.eci.dosw.brigades_and_incidents.entity.Incidente;
import edu.eci.dosw.brigades_and_incidents.event.IncidenteAuditPublisher;
import edu.eci.dosw.brigades_and_incidents.exception.EvidenciaIncidenteException;
import edu.eci.dosw.brigades_and_incidents.repository.BrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.EvidenciaIncidenteRepository;
import edu.eci.dosw.brigades_and_incidents.repository.IncidenteRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private static final Set<String> ROLES_COORDINADOR = Set.of(
            "COORDINADOR",
            "ORGANIZADOR",
            "ADMIN"
    );

    private static final String ROL_BRIGADISTA = "BRIGADISTA";
    private static final String ROL_AUDITOR = "AUDITOR";
    private static final String ESTADO_CERRADO = "CERRADO";
    private static final String ESTADO_ACTIVO = "ACTIVO";

    private final EvidenciaIncidenteRepository evidenciaRepository;
    private final IncidenteRepository incidenteRepository;
    private final BrigadistaRepository brigadistaRepository;
    private final EvidenciaStorage evidenciaStorage;
    private final IncidenteAuditPublisher auditPublisher;
    private final UsuarioClient usuarioClient;

    public EvidenciaIncidenteService(
            EvidenciaIncidenteRepository evidenciaRepository,
            IncidenteRepository incidenteRepository,
            BrigadistaRepository brigadistaRepository,
            EvidenciaStorage evidenciaStorage,
            IncidenteAuditPublisher auditPublisher,
            UsuarioClient usuarioClient
    ) {
        this.evidenciaRepository = evidenciaRepository;
        this.incidenteRepository = incidenteRepository;
        this.brigadistaRepository = brigadistaRepository;
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
        // 1. Validar la identidad del usuario.
        validarIdentidad(usuarioId);

        // 2. Buscar el incidente.
        Incidente incidente = buscarIncidente(eventoId, incidenteId);

        // 3. Verificar que el incidente no esté cerrado.
        validarIncidenteAbierto(incidente);

        // 4. Consultar usuario y validar permisos sobre el incidente.
        validarPermisos(usuarioId, incidente);

        // 5. Validar el archivo.
        validarArchivo(archivo);

        // 6. Verificar el límite de evidencias.
        if (evidenciaRepository.countByIncidenteId(incidenteId)
                >= MAXIMO_EVIDENCIAS) {
            throw new EvidenciaIncidenteException(
                    "Un incidente no puede tener más de 5 evidencias."
            );
        }

        // 7. Guardar el archivo.
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

            // 8. Registrar auditoría.
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
        // Obtener la identidad establecida por el filtro de seguridad.
        String usuarioId = obtenerUsuarioAutenticado();

        // Verificar que el incidente exista y pertenezca al evento.
        buscarIncidente(eventoId, incidenteId);

        // Verificar que el usuario tenga permiso de consulta.
        validarPermisosConsulta(usuarioId);

        return evidenciaRepository.findByIncidenteId(incidenteId)
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    private String obtenerUsuarioAutenticado() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "No se pudo identificar al usuario autenticado."
            );
        }

        return authentication.getName();
    }

    private void validarIdentidad(String usuarioId) {

        if (usuarioId == null || usuarioId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "No se pudo identificar al usuario autenticado."
            );
        }
    }

    private Incidente buscarIncidente(
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

        return incidente;
    }

    private void validarIncidenteAbierto(Incidente incidente) {

        if (incidente.getEstado() == null
                || ESTADO_CERRADO.equalsIgnoreCase(
                        incidente.getEstado().getNombre()
                )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pueden adjuntar evidencias a un incidente cerrado."
            );
        }
    }

    private void validarPermisos(
            String usuarioId,
            Incidente incidente
    ) {
        // Consultar usuario en MS01.
        UsuarioClient.UsuarioExterno usuario =
                usuarioClient.consultarUsuario(usuarioId);

        validarUsuarioActivo(usuario);

        Set<String> roles = obtenerRoles(usuario);

        // Los auditores no pueden cargar evidencias.
        if (roles.contains(ROL_AUDITOR)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Los usuarios con rol auditor no pueden cargar evidencias."
            );
        }

        boolean esPropietario = usuarioId.equals(
                incidente.getReportanteId()
        );

        boolean esUsuarioGeneral = roles.stream()
                .anyMatch(ROLES_USUARIO_GENERAL::contains);

        boolean esBrigadista = roles.contains(ROL_BRIGADISTA);

        if (esPropietario) {

            if (!esUsuarioGeneral && !esBrigadista) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "El usuario no tiene permisos para adjuntar evidencias."
                );
            }

        } else {

            if (!esBrigadista) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Solo un brigadista activo puede adjuntar evidencias "
                                + "a incidentes de otros usuarios."
                );
            }

            validarBrigadistaActivo(usuarioId);
        }
    }

    private void validarPermisosConsulta(String usuarioId) {

        UsuarioClient.UsuarioExterno usuario =
                usuarioClient.consultarUsuario(usuarioId);

        validarUsuarioActivo(usuario);

        Set<String> roles = obtenerRoles(usuario);

        // El auditor puede consultar evidencias.
        if (roles.contains(ROL_AUDITOR)) {
            return;
        }

        // El coordinador puede consultar evidencias.
        if (roles.stream().anyMatch(ROLES_COORDINADOR::contains)) {
            return;
        }

        // El brigadista debe estar registrado y activo.
        if (roles.contains(ROL_BRIGADISTA)) {
            validarBrigadistaActivo(usuarioId);
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tiene permisos para consultar las evidencias del incidente."
        );
    }

    private void validarUsuarioActivo(
            UsuarioClient.UsuarioExterno usuario
    ) {

        if (usuario == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El usuario no existe en MS01."
            );
        }

        if (usuario.estado() == null
                || !ESTADO_ACTIVO.equalsIgnoreCase(usuario.estado())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El usuario se encuentra inactivo."
            );
        }
    }

    private Set<String> obtenerRoles(
            UsuarioClient.UsuarioExterno usuario
    ) {

        return usuario.roles() == null
                ? Set.of()
                : usuario.roles().stream()
                        .filter(rol -> rol != null && !rol.isBlank())
                        .map(rol -> rol.trim().toUpperCase(Locale.ROOT))
                        .collect(Collectors.toSet());
    }

    private void validarBrigadistaActivo(String usuarioId) {

        Brigadista brigadista = brigadistaRepository
                .findByUsuarioIdExterno(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "El usuario no está registrado como brigadista."
                ));

        if (brigadista.getEstado() == null
                || !ESTADO_ACTIVO.equalsIgnoreCase(
                        brigadista.getEstado().getNombre()
                )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El brigadista debe estar activo para realizar esta operación."
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

        // Obtener el nombre una sola vez y validar que no sea nulo.
        String nombreArchivo = archivo.getOriginalFilename();

        if (nombreArchivo == null || nombreArchivo.isBlank()) {
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
