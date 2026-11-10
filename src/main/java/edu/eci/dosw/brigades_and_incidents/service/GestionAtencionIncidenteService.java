
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.client.UsuarioClient;
import edu.eci.dosw.brigades_and_incidents.dto.AsignarResponsableRequest;
import edu.eci.dosw.brigades_and_incidents.dto.CambioEstadoIncidenteResponse;
import edu.eci.dosw.brigades_and_incidents.dto.HistorialIncidenteResponse;
import edu.eci.dosw.brigades_and_incidents.dto.ResolverIncidenteRequest;
import edu.eci.dosw.brigades_and_incidents.entity.*;
import edu.eci.dosw.brigades_and_incidents.event.IncidenteAuditPublisher;
import edu.eci.dosw.brigades_and_incidents.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class GestionAtencionIncidenteService {

    private static final String ROL_BRIGADISTA = "BRIGADISTA";

    private static final Set<String> ROLES_COORDINADOR = Set.of(
            "COORDINADOR",
            "ORGANIZADOR"
    );

    private final IncidenteRepository incidenteRepository;
    private final EstadoIncidenteRepository estadoRepository;
    private final BrigadistaRepository brigadistaRepository;
    private final AsignacionBrigadistaRepository asignacionRepository;
    private final HistorialIncidenteRepository historialRepository;
    private final UsuarioClient usuarioClient;
    private final IncidenteAuditPublisher auditPublisher;

    public GestionAtencionIncidenteService(
            IncidenteRepository incidenteRepository,
            EstadoIncidenteRepository estadoRepository,
            BrigadistaRepository brigadistaRepository,
            AsignacionBrigadistaRepository asignacionRepository,
            HistorialIncidenteRepository historialRepository,
            UsuarioClient usuarioClient,
            IncidenteAuditPublisher auditPublisher
    ) {
        this.incidenteRepository = incidenteRepository;
        this.estadoRepository = estadoRepository;
        this.brigadistaRepository = brigadistaRepository;
        this.asignacionRepository = asignacionRepository;
        this.historialRepository = historialRepository;
        this.usuarioClient = usuarioClient;
        this.auditPublisher = auditPublisher;
    }

    /**
     * Asigna o reasigna el responsable de un incidente.
     * La operación está reservada para coordinadores y organizadores.
     */
    @Transactional
    public CambioEstadoIncidenteResponse asignarResponsable(
            String eventoId,
            UUID incidenteId,
            AsignarResponsableRequest request,
            String actorId
    ) {
        validarIdentidad(actorId);
        validarCoordinador(actorId);

        if (request == null || request.responsableId() == null
                || request.responsableId().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del responsable es obligatorio."
            );
        }

        Incidente incidente = obtenerIncidente(eventoId, incidenteId);
        String estadoActual = obtenerEstado(incidente);

        if ("RESUELTO".equals(estadoActual)
                || "CERRADO".equals(estadoActual)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede reasignar un incidente resuelto o cerrado."
            );
        }

        String responsableNuevo = request.responsableId().trim();

        validarBrigadistaAsignado(
                responsableNuevo,
                incidente
        );

        String responsableAnterior =
                incidente.getBrigadistaResponsableId();

        if (responsableNuevo.equals(responsableAnterior)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El brigadista indicado ya es el responsable del incidente."
            );
        }

        EstadoIncidente estadoAnterior = incidente.getEstado();

        // Un incidente reportado pasa a asignado al recibir responsable.
        if ("REPORTADO".equals(estadoActual)) {
            incidente.setEstado(obtenerEstadoCatalogo("ASIGNADO"));
        }

        incidente.setBrigadistaResponsableId(responsableNuevo);

        Incidente actualizado = incidenteRepository.save(incidente);

        HistorialIncidente historial = registrarHistorial(
                actualizado,
                estadoAnterior,
                actualizado.getEstado(),
                responsableAnterior,
                responsableNuevo,
                actorId,
                request.observaciones()
        );

        auditPublisher.publicar(
                "ASIGNAR_RESPONSABLE",
                "INCIDENTE",
                incidenteId.toString(),
                eventoId,
                actorId,
                "EXITOSO",
                "Responsable asignado o reasignado: " + responsableNuevo
        );

        return construirRespuesta(
                actualizado,
                estadoAnterior.getNombre(),
                historial.getFechaCambio()
        );
    }

    /**
     * Inicia la atención de un incidente.
     * Solo puede hacerlo el brigadista responsable.
     */
    @Transactional
    public CambioEstadoIncidenteResponse iniciarAtencion(
            String eventoId,
            UUID incidenteId,
            String actorId
    ) {
        validarIdentidad(actorId);

        Incidente incidente = obtenerIncidente(eventoId, incidenteId);

        validarBrigadistaResponsable(actorId, incidente);

        validarEstadoActual(incidente, "ASIGNADO");

        EstadoIncidente estadoAnterior = incidente.getEstado();
        EstadoIncidente nuevoEstado = obtenerEstadoCatalogo("EN_ATENCION");

        OffsetDateTime ahora = OffsetDateTime.now();

        incidente.setEstado(nuevoEstado);

        if (incidente.getFechaPrimeraAtencion() == null) {
            incidente.setFechaPrimeraAtencion(ahora);
        }

        Incidente actualizado = incidenteRepository.save(incidente);

        HistorialIncidente historial = registrarHistorial(
                actualizado,
                estadoAnterior,
                nuevoEstado,
                actorId,
                actorId,
                actorId,
                "El brigadista inició la atención del incidente."
        );

        auditPublisher.publicar(
                "INICIAR_ATENCION",
                "INCIDENTE",
                incidenteId.toString(),
                eventoId,
                actorId,
                "EXITOSO",
                "Se inició la atención del incidente."
        );

        return construirRespuesta(
                actualizado,
                estadoAnterior.getNombre(),
                historial.getFechaCambio()
        );
    }

    /**
     * Resuelve un incidente.
     * Solo puede hacerlo el brigadista responsable.
     */
    @Transactional
    public CambioEstadoIncidenteResponse resolverIncidente(
            String eventoId,
            UUID incidenteId,
            ResolverIncidenteRequest request,
            String actorId
    ) {
        validarIdentidad(actorId);

        if (request == null || request.observaciones() == null
                || request.observaciones().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Las observaciones de resolución son obligatorias."
            );
        }

        Incidente incidente = obtenerIncidente(eventoId, incidenteId);

        validarBrigadistaResponsable(actorId, incidente);

        validarEstadoActual(incidente, "EN_ATENCION");

        EstadoIncidente estadoAnterior = incidente.getEstado();
        EstadoIncidente nuevoEstado = obtenerEstadoCatalogo("RESUELTO");

        incidente.setEstado(nuevoEstado);
        incidente.setFechaResolucion(OffsetDateTime.now());
        incidente.setObservacionesResolucion(
                request.observaciones().trim()
        );

        Incidente actualizado = incidenteRepository.save(incidente);

        HistorialIncidente historial = registrarHistorial(
                actualizado,
                estadoAnterior,
                nuevoEstado,
                actorId,
                actorId,
                actorId,
                request.observaciones().trim()
        );

        auditPublisher.publicar(
                "RESOLVER_INCIDENTE",
                "INCIDENTE",
                incidenteId.toString(),
                eventoId,
                actorId,
                "EXITOSO",
                "El brigadista resolvió el incidente."
        );

        return construirRespuesta(
                actualizado,
                estadoAnterior.getNombre(),
                historial.getFechaCambio()
        );
    }

    /**
     * Cierra un incidente que ya fue resuelto.
     * Solo puede hacerlo un coordinador u organizador.
     */
    @Transactional
    public CambioEstadoIncidenteResponse cerrarIncidente(
            String eventoId,
            UUID incidenteId,
            String actorId
    ) {
        validarIdentidad(actorId);
        validarCoordinador(actorId);

        Incidente incidente = obtenerIncidente(eventoId, incidenteId);

        validarEstadoActual(incidente, "RESUELTO");

        EstadoIncidente estadoAnterior = incidente.getEstado();
        EstadoIncidente nuevoEstado = obtenerEstadoCatalogo("CERRADO");

        incidente.setEstado(nuevoEstado);
        incidente.setFechaCierre(OffsetDateTime.now());

        Incidente actualizado = incidenteRepository.save(incidente);

        HistorialIncidente historial = registrarHistorial(
                actualizado,
                estadoAnterior,
                nuevoEstado,
                actualizado.getBrigadistaResponsableId(),
                actualizado.getBrigadistaResponsableId(),
                actorId,
                "El coordinador cerró el incidente."
        );

        auditPublisher.publicar(
                "CERRAR_INCIDENTE",
                "INCIDENTE",
                incidenteId.toString(),
                eventoId,
                actorId,
                "EXITOSO",
                "El coordinador cerró el incidente."
        );

        return construirRespuesta(
                actualizado,
                estadoAnterior.getNombre(),
                historial.getFechaCambio()
        );
    }

    /**
     * Consulta el historial cronológico de un incidente.
     */
    @Transactional(readOnly = true)
    public List<HistorialIncidenteResponse> consultarHistorial(
            String eventoId,
            UUID incidenteId,
            String actorId
    ) {
        validarIdentidad(actorId);

        Incidente incidente = obtenerIncidente(eventoId, incidenteId);

        UsuarioClient.UsuarioExterno usuario =
                consultarUsuarioActivo(actorId);

        Set<String> roles = obtenerRoles(usuario);

        // Los auditores pueden consultar el historial.
        if (roles.contains("AUDITOR")) {
            return obtenerHistorial(incidenteId);
        }

        // Los brigadistas solo pueden consultar incidentes
        // de eventos y ubicaciones donde tengan asignación vigente.
        if (roles.contains(ROL_BRIGADISTA)) {
            validarBrigadistaAsignado(actorId, incidente);
            return obtenerHistorial(incidenteId);
        }

        // Coordinadores, organizadores y administradores.
        if (roles.stream().anyMatch(ROLES_COORDINADOR::contains)
                || roles.contains("ADMIN")) {
            return obtenerHistorial(incidenteId);
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "El usuario no tiene permisos para consultar el historial."
        );
    }

    private List<HistorialIncidenteResponse> obtenerHistorial(
            UUID incidenteId
    ) {
        return historialRepository
                .findByIncidente_IdOrderByFechaCambioAsc(incidenteId)
                .stream()
                .map(historial -> new HistorialIncidenteResponse(
                        historial.getId(),
                        historial.getEstadoAnterior() == null
                                ? null
                                : historial.getEstadoAnterior().getNombre(),
                        historial.getEstadoNuevo().getNombre(),
                        historial.getResponsableAnteriorId(),
                        historial.getResponsableNuevoId(),
                        historial.getActorId(),
                        historial.getObservaciones(),
                        historial.getFechaCambio()
                ))
                .toList();
    }

    private Incidente obtenerIncidente(
            String eventoId,
            UUID incidenteId
    ) {
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

        return incidente;
    }

    private void validarBrigadistaAsignado(
            String responsableId,
            Incidente incidente
    ) {
        UsuarioClient.UsuarioExterno usuario =
                consultarUsuarioActivo(responsableId);

        if (!obtenerRoles(usuario).contains(ROL_BRIGADISTA)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El responsable debe tener el rol BRIGADISTA en MS01."
            );
        }

        Brigadista brigadista = brigadistaRepository
                .findByUsuarioIdExterno(responsableId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "El usuario no está registrado como brigadista en MS05."
                ));

        if (brigadista.getEstado() == null
                || !"ACTIVO".equalsIgnoreCase(
                        brigadista.getEstado().getNombre()
                )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El brigadista debe estar activo en MS05."
            );
        }

        boolean tieneAsignacionVigente = asignacionRepository
                .findByEventoIdAndEstado_Nombre(
                        incidente.getEventoId(),
                        "VIGENTE"
                )
                .stream()
                .anyMatch(asignacion ->
                        responsableId.equals(asignacion.getBrigadistaId())
                        && incidente.getUbicacionId().equals(
                                asignacion.getUbicacionId()
                        )
                        && incidente.getTipoUbicacion().getNombre().equals(
                                asignacion.getTipoUbicacion().getNombre()
                        )
                );

        if (!tieneAsignacionVigente) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El brigadista no tiene una asignación vigente en la ubicación del incidente."
            );
        }
    }

    private void validarBrigadistaResponsable(
            String actorId,
            Incidente incidente
    ) {
        UsuarioClient.UsuarioExterno usuario =
                consultarUsuarioActivo(actorId);

        if (!obtenerRoles(usuario).contains(ROL_BRIGADISTA)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un brigadista puede realizar esta operación."
            );
        }

        if (incidente.getBrigadistaResponsableId() == null
                || !incidente.getBrigadistaResponsableId().equals(actorId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo el brigadista responsable puede atender este incidente."
            );
        }

        validarBrigadistaAsignado(actorId, incidente);
    }

    private void validarCoordinador(String actorId) {
        UsuarioClient.UsuarioExterno usuario =
                consultarUsuarioActivo(actorId);

        Set<String> roles = obtenerRoles(usuario);

        if (roles.stream().noneMatch(ROLES_COORDINADOR::contains)
                && !roles.contains("ADMIN")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "La operación requiere el rol COORDINADOR u ORGANIZADOR."
            );
        }
    }

    private UsuarioClient.UsuarioExterno consultarUsuarioActivo(
            String usuarioId
    ) {
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
                    "El usuario se encuentra inactivo en MS01."
            );
        }

        return usuario;
    }

    private Set<String> obtenerRoles(
            UsuarioClient.UsuarioExterno usuario
    ) {
        if (usuario.roles() == null) {
            return Set.of();
        }

        return usuario.roles().stream()
                .filter(rol -> rol != null && !rol.isBlank())
                .map(rol -> rol.trim().toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
    }

    private void validarIdentidad(String actorId) {
        if (actorId == null || actorId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "No se pudo identificar al usuario autenticado."
            );
        }
    }

    private void validarEstadoActual(
            Incidente incidente,
            String estadoEsperado
    ) {
        String estadoActual = obtenerEstado(incidente);

        if (!estadoEsperado.equals(estadoActual)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transición inválida. El incidente debe estar en estado "
                            + estadoEsperado + " y actualmente está en "
                            + estadoActual + "."
            );
        }
    }

    private String obtenerEstado(Incidente incidente) {
        if (incidente.getEstado() == null
                || incidente.getEstado().getNombre() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El incidente no tiene un estado válido."
            );
        }

        return incidente.getEstado().getNombre()
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private EstadoIncidente obtenerEstadoCatalogo(String nombre) {
        return estadoRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el estado de incidente: " + nombre
                ));
    }

    /**
     * Registra el cambio y devuelve el historial persistido.
     */
    private HistorialIncidente registrarHistorial(
            Incidente incidente,
            EstadoIncidente estadoAnterior,
            EstadoIncidente estadoNuevo,
            String responsableAnterior,
            String responsableNuevo,
            String actorId,
            String observaciones
    ) {
        HistorialIncidente historial = new HistorialIncidente();

        historial.setIncidente(incidente);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoNuevo);
        historial.setResponsableAnteriorId(responsableAnterior);
        historial.setResponsableNuevoId(responsableNuevo);
        historial.setActorId(actorId);
        historial.setObservaciones(observaciones);
        historial.setFechaCambio(OffsetDateTime.now());

        return historialRepository.save(historial);
    }

    /**
     * Construye la respuesta utilizando la fecha real del historial.
     */
    private CambioEstadoIncidenteResponse construirRespuesta(
            Incidente incidente,
            String estadoAnterior,
            OffsetDateTime fechaCambio
    ) {
        return new CambioEstadoIncidenteResponse(
                incidente.getId(),
                estadoAnterior,
                obtenerEstado(incidente),
                incidente.getBrigadistaResponsableId(),
                fechaCambio
        );
    }
}
