
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.client.UbicacionClient;
import edu.eci.dosw.brigades_and_incidents.client.UsuarioClient;
import edu.eci.dosw.brigades_and_incidents.dto.CrearIncidenteRequest;
import edu.eci.dosw.brigades_and_incidents.dto.IncidenteResponse;
import edu.eci.dosw.brigades_and_incidents.entity.*;
import edu.eci.dosw.brigades_and_incidents.exception.Ms01IntegrationException;
import edu.eci.dosw.brigades_and_incidents.mapper.IncidenteMapper;
import edu.eci.dosw.brigades_and_incidents.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;
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
    private final PrioridadIncidenteRepository prioridadRepository;
    private final EstadoIncidenteRepository estadoRepository;
    private final TipoUbicacionRepository tipoUbicacionRepository;
    private final AsignacionBrigadistaRepository asignacionRepository;
    private final BrigadistaRepository brigadistaRepository;
    private final GestionOperacionEventoService gestionEventoService;
    private final UbicacionClient ubicacionClient;
    private final UsuarioClient usuarioClient;
    private final IncidenteMapper incidenteMapper;

    public IncidenteService(
            IncidenteRepository incidenteRepository,
            PrioridadIncidenteRepository prioridadRepository,
            EstadoIncidenteRepository estadoRepository,
            TipoUbicacionRepository tipoUbicacionRepository,
            AsignacionBrigadistaRepository asignacionRepository,
            BrigadistaRepository brigadistaRepository,
            GestionOperacionEventoService gestionEventoService,
            UbicacionClient ubicacionClient,
            UsuarioClient usuarioClient,
            IncidenteMapper incidenteMapper
    ) {
        this.incidenteRepository = incidenteRepository;
        this.prioridadRepository = prioridadRepository;
        this.estadoRepository = estadoRepository;
        this.tipoUbicacionRepository = tipoUbicacionRepository;
        this.asignacionRepository = asignacionRepository;
        this.brigadistaRepository = brigadistaRepository;
        this.gestionEventoService = gestionEventoService;
        this.ubicacionClient = ubicacionClient;
        this.usuarioClient = usuarioClient;
        this.incidenteMapper = incidenteMapper;
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

            // El brigadista puede registrar una solicitud para otra persona.
            validarBrigadistaActivo(reportanteId);
            nombreAfectado = request.nombreAfectado().trim();

        } else {

            // Si no se especifica otra persona, el incidente es propio.
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

        boolean hayBrigadista = hayBrigadistaActivoAsignado(
                eventoId,
                ubicacion.id(),
                ubicacion.tipo()
        );

        EstadoIncidente estado = estadoRepository
                .findByNombre(hayBrigadista ? "ASIGNADO" : "REPORTADO")
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

        Incidente incidenteGuardado =
                incidenteRepository.save(incidente);

        return incidenteMapper.toResponse(incidenteGuardado);
    }

    private void validarIdentidad(String reportanteId) {
        if (reportanteId == null || reportanteId.isBlank()) {
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

    private void validarBrigadistaActivo(String reportanteId) {
        Brigadista brigadista = brigadistaRepository
                .findByUsuarioIdExterno(reportanteId)
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
                    "El brigadista debe estar activo para registrar incidentes en nombre de otra persona."
            );
        }
    }

    private boolean hayBrigadistaActivoAsignado(
            String eventoId,
            String ubicacionId,
            String tipoUbicacion
    ) {
        return asignacionRepository
                .findByEventoIdAndEstado_Nombre(eventoId, "VIGENTE")
                .stream()
                .filter(asignacion ->
                        ubicacionId.equals(asignacion.getUbicacionId())
                )
                .filter(asignacion ->
                        tipoUbicacion.equals(
                                asignacion.getTipoUbicacion().getNombre()
                        )
                )
                .anyMatch(asignacion ->
                        brigadistaRepository
                                .findByUsuarioIdExterno(
                                        asignacion.getBrigadistaId()
                                )
                                .map(brigadista ->
                                        brigadista.getEstado() != null
                                        && "ACTIVO".equalsIgnoreCase(
                                                brigadista.getEstado().getNombre()
                                        )
                                )
                                .orElse(false)
                );
    }
}
