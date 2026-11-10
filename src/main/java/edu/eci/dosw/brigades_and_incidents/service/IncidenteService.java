
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.client.UbicacionClient;
import edu.eci.dosw.brigades_and_incidents.client.UsuarioClient;
import edu.eci.dosw.brigades_and_incidents.dto.CrearIncidenteRequest;
import edu.eci.dosw.brigades_and_incidents.dto.EditarIncidenteRequest;
import edu.eci.dosw.brigades_and_incidents.dto.IncidenteResponse;
import edu.eci.dosw.brigades_and_incidents.entity.*;
import edu.eci.dosw.brigades_and_incidents.event.IncidenteAuditPublisher;
import edu.eci.dosw.brigades_and_incidents.exception.Ms01IntegrationException;
import edu.eci.dosw.brigades_and_incidents.mapper.IncidenteMapper;
import edu.eci.dosw.brigades_and_incidents.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class IncidenteService {

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

    private final IncidenteRepository incidenteRepository;
    private final HistorialIncidenteRepository historialRepository;
    private final PrioridadIncidenteRepository prioridadRepository;
    private final EstadoIncidenteRepository estadoRepository;
    private final TipoUbicacionRepository tipoUbicacionRepository;
    private final AsignacionBrigadistaRepository asignacionRepository;
    private final BrigadistaRepository brigadistaRepository;
    private final GestionOperacionEventoService gestionEventoService;
    private final UbicacionClient ubicacionClient;
    private final UsuarioClient usuarioClient;
    private final IncidenteMapper incidenteMapper;
    private final IncidenteAuditPublisher auditPublisher;

    public IncidenteService(
            IncidenteRepository incidenteRepository,
            HistorialIncidenteRepository historialRepository,
            PrioridadIncidenteRepository prioridadRepository,
            EstadoIncidenteRepository estadoRepository,
            TipoUbicacionRepository tipoUbicacionRepository,
            AsignacionBrigadistaRepository asignacionRepository,
            BrigadistaRepository brigadistaRepository,
            GestionOperacionEventoService gestionEventoService,
            UbicacionClient ubicacionClient,
            UsuarioClient usuarioClient,
            IncidenteMapper incidenteMapper,
            IncidenteAuditPublisher auditPublisher
    ) {
        this.incidenteRepository = incidenteRepository;
        this.historialRepository = historialRepository;
        this.prioridadRepository = prioridadRepository;
        this.estadoRepository = estadoRepository;
        this.tipoUbicacionRepository = tipoUbicacionRepository;
        this.asignacionRepository = asignacionRepository;
        this.brigadistaRepository = brigadistaRepository;
        this.gestionEventoService = gestionEventoService;
        this.ubicacionClient = ubicacionClient;
        this.usuarioClient = usuarioClient;
        this.incidenteMapper = incidenteMapper;
        this.auditPublisher = auditPublisher;
    }

    @Transactional
    public IncidenteResponse registrarIncidente(
            String eventoId,
            CrearIncidenteRequest request,
            String reportanteId
    ) {
        validarIdentidad(reportanteId);

        UsuarioClient.UsuarioExterno usuario =
                usuarioClient.consultarUsuario(reportanteId);

        validarUsuarioExistenteYActivo(usuario);

        Set<String> roles = usuario.roles() == null
                ? Set.of()
                : usuario.roles().stream()
                        .filter(rol -> rol != null && !rol.isBlank())
                        .map(rol -> rol.trim().toUpperCase(Locale.ROOT))
                        .collect(Collectors.toSet());

        validarPermisosRegistro(roles);

        boolean esBrigadista = roles.contains(ROL_BRIGADISTA);

        String nombreAfectado;

        if (esBrigadista && request.nombreAfectado() != null
                && !request.nombreAfectado().isBlank()) {

            validarBrigadistaActivo(reportanteId);
            nombreAfectado = request.nombreAfectado().trim();

        } else {

            nombreAfectado = usuario.nombreCompleto();

            if (nombreAfectado == null || nombreAfectado.isBlank()) {
                throw new Ms01IntegrationException(
                        "MS01 no devolvió el nombre completo del usuario."
                );
            }
        }

        validarEventoActivo(eventoId);

        UbicacionClient.UbicacionExterna ubicacion =
                validarUbicacion(request);

        TipoUbicacion tipoUbicacion = tipoUbicacionRepository
                .findByNombre(request.tipoUbicacion().trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El tipo de ubicación no está registrado."
                        )
                );

        PrioridadIncidente prioridad = prioridadRepository
                .findById(request.prioridadId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "La prioridad seleccionada no existe."
                        )
                );

        // Buscar un brigadista activo y asignado a esta ubicación.
        String responsableId = seleccionarBrigadistaResponsable(
                eventoId,
                ubicacion.id(),
                ubicacion.tipo()
        );

        // El estado inicial depende de si se encontró un responsable.
        EstadoIncidente estado = estadoRepository
                .findByNombre(responsableId != null ? "ASIGNADO" : "REPORTADO")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No se encuentra el estado inicial del incidente."
                        )
                );

        Incidente incidente = new Incidente();
        incidente.setEventoId(eventoId);
        incidente.setNombreAfectado(nombreAfectado);
        incidente.setReportanteId(reportanteId);
        incidente.setUbicacionId(ubicacion.id());
        incidente.setTipoUbicacion(tipoUbicacion);
        incidente.setPrioridad(prioridad);
        incidente.setEstado(estado);
        incidente.setDescripcion(request.descripcion());
        incidente.setBrigadistaResponsableId(responsableId);

        Incidente incidenteGuardado =
                incidenteRepository.save(incidente);

        // Registrar el estado inicial en el historial.
        registrarHistorialInicial(
                incidenteGuardado,
                estado,
                responsableId,
                reportanteId
        );

        IncidenteResponse response =
                incidenteMapper.toResponse(incidenteGuardado);

        auditPublisher.publicar(
                "CREAR_INCIDENTE",
                "INCIDENTE",
                incidenteGuardado.getId().toString(),
                eventoId,
                reportanteId,
                "EXITOSO",
                responsableId != null
                        ? "Se registró el incidente y se asignó automáticamente un brigadista responsable."
                        : "Se registró el incidente sin brigadista responsable disponible."
        );

        return response;
    }

    /**
     * Registra el estado con el que se creó el incidente.
     */
    private void registrarHistorialInicial(
            Incidente incidente,
            EstadoIncidente estadoInicial,
            String responsableId,
            String actorId
    ) {
        HistorialIncidente historial = new HistorialIncidente();

        historial.setIncidente(incidente);
        historial.setEstadoAnterior(null);
        historial.setEstadoNuevo(estadoInicial);
        historial.setResponsableAnteriorId(null);
        historial.setResponsableNuevoId(responsableId);
        historial.setActorId(actorId);
        historial.setObservaciones(
                "Registro inicial del incidente."
        );

        historialRepository.save(historial);
    }

    /**
     * Edita la descripción de un incidente existente.
     * El propietario puede editar su incidente y un brigadista activo
     * puede editar incidentes reportados por otros usuarios.
     */
    @Transactional
    public IncidenteResponse editarIncidente(
            String eventoId,
            UUID incidenteId,
            EditarIncidenteRequest request,
            String usuarioId
    ) {
        validarIdentidad(usuarioId);

        Incidente incidente = incidenteRepository.findById(incidenteId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El incidente no existe."
                ));

        if (!incidente.getEventoId().equals(eventoId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El incidente no pertenece al evento indicado."
            );
        }

        if (incidente.getEstado() == null
                || "CERRADO".equalsIgnoreCase(
                        incidente.getEstado().getNombre()
                )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede editar un incidente cerrado."
            );
        }

        UsuarioClient.UsuarioExterno usuario =
                usuarioClient.consultarUsuario(usuarioId);

        validarUsuarioExistenteYActivo(usuario);

        Set<String> roles = usuario.roles() == null
                ? Set.of()
                : usuario.roles().stream()
                        .filter(rol -> rol != null && !rol.isBlank())
                        .map(rol -> rol.trim().toUpperCase(Locale.ROOT))
                        .collect(Collectors.toSet());

        if (roles.contains(ROL_AUDITOR)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Los usuarios con rol auditor no pueden editar incidentes."
            );
        }

        boolean esPropietario =
                incidente.getReportanteId().equals(usuarioId);

        boolean esUsuarioGeneral = roles.stream()
                .anyMatch(ROLES_USUARIO_GENERAL::contains);

        boolean esBrigadista = roles.contains(ROL_BRIGADISTA);

        if (esPropietario) {

            if (!esUsuarioGeneral && !esBrigadista) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "El usuario no tiene permisos para editar este incidente."
                );
            }

        } else {

            if (!esBrigadista) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Solo un brigadista activo puede editar incidentes de otros usuarios."
                );
            }

            validarBrigadistaActivo(usuarioId);
        }

        if (request != null && request.descripcion() != null) {
            incidente.setDescripcion(request.descripcion().trim());
        }

        Incidente actualizado = incidenteRepository.save(incidente);

        auditPublisher.publicar(
                "EDITAR_INCIDENTE",
                "INCIDENTE",
                actualizado.getId().toString(),
                eventoId,
                usuarioId,
                "EXITOSO",
                "Se actualizó la descripción del incidente."
        );

        return incidenteMapper.toResponse(actualizado);
    }

    private void validarIdentidad(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "No se pudo identificar al usuario autenticado."
            );
        }
    }

    private void validarUsuarioExistenteYActivo(
            UsuarioClient.UsuarioExterno usuario
    ) {
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
    }

    private void validarPermisosRegistro(Set<String> roles) {

        if (roles.contains(ROL_AUDITOR)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Los usuarios con rol auditor no pueden registrar incidentes."
            );
        }

        boolean usuarioGeneralAutorizado = roles.stream()
                .anyMatch(ROLES_USUARIO_GENERAL::contains);

        boolean esBrigadista = roles.contains(ROL_BRIGADISTA);

        if (!usuarioGeneralAutorizado && !esBrigadista) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El usuario no tiene un rol autorizado para registrar incidentes."
            );
        }
    }

    private void validarEventoActivo(String eventoId) {
        if (eventoId == null || eventoId.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador del evento es obligatorio."
            );
        }

        ConfiguracionOperacionEvento configuracion =
                gestionEventoService.obtenerConfiguracion(eventoId);

        if (configuracion == null || !configuracion.isPanelHabilitado()) {
            throw new IllegalStateException(
                    "No se pueden registrar incidentes porque el evento no está en ejecución."
            );
        }
    }

    private UbicacionClient.UbicacionExterna validarUbicacion(
            CrearIncidenteRequest request
    ) {
        UbicacionClient.UbicacionExterna ubicacion =
                ubicacionClient.consultarUbicacion(
                        request.ubicacionId(),
                        request.tipoUbicacion()
                );

        if (ubicacion == null || !ubicacion.activa()) {
            throw new IllegalArgumentException(
                    "La ubicación no existe o no se encuentra activa."
            );
        }

        return ubicacion;
    }

    private void validarBrigadistaActivo(String usuarioId) {
        Brigadista brigadista = brigadistaRepository
                .findByUsuarioIdExterno(usuarioId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "El usuario no está registrado como brigadista en MS05."
                        )
                );

        if (brigadista.getEstado() == null
                || !"ACTIVO".equalsIgnoreCase(
                        brigadista.getEstado().getNombre()
                )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El brigadista debe estar activo para realizar esta operación."
            );
        }
    }

    /**
     * Busca un brigadista con asignación vigente para el evento y ubicación.
     * Comprueba que esté activo en MS05 y que MS01 confirme su rol y estado.
     *
     * @return identificador externo del brigadista o null si no hay uno válido.
     */
    private String seleccionarBrigadistaResponsable(
            String eventoId,
            String ubicacionId,
            String tipoUbicacion
    ) {
        var asignaciones = asignacionRepository
                .findByEventoIdAndEstado_Nombre(eventoId, "VIGENTE");

        for (AsignacionBrigadista asignacion : asignaciones) {

            if (asignacion.getUbicacionId() == null
                    || !ubicacionId.equals(asignacion.getUbicacionId())) {
                continue;
            }

            if (asignacion.getTipoUbicacion() == null
                    || asignacion.getTipoUbicacion().getNombre() == null
                    || !tipoUbicacion.equalsIgnoreCase(
                            asignacion.getTipoUbicacion().getNombre()
                    )) {
                continue;
            }

            String brigadistaId = asignacion.getBrigadistaId();

            if (brigadistaId == null || brigadistaId.isBlank()) {
                continue;
            }

            Brigadista brigadista = brigadistaRepository
                    .findByUsuarioIdExterno(brigadistaId)
                    .orElse(null);

            if (brigadista == null
                    || brigadista.getEstado() == null
                    || !"ACTIVO".equalsIgnoreCase(
                            brigadista.getEstado().getNombre()
                    )) {
                continue;
            }

            UsuarioClient.UsuarioExterno usuario =
                    usuarioClient.consultarUsuario(brigadistaId);

            if (usuario == null
                    || usuario.estado() == null
                    || !"ACTIVO".equalsIgnoreCase(usuario.estado())) {
                continue;
            }

            Set<String> roles = usuario.roles() == null
                    ? Set.of()
                    : usuario.roles().stream()
                            .filter(rol -> rol != null && !rol.isBlank())
                            .map(rol -> rol.trim().toUpperCase(Locale.ROOT))
                            .collect(Collectors.toSet());

            if (roles.contains(ROL_BRIGADISTA)) {
                return brigadistaId;
            }
        }

        return null;
    }
}
