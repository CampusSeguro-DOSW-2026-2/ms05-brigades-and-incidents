
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.client.UbicacionClient;
import edu.eci.dosw.brigades_and_incidents.client.UsuarioClient;
import edu.eci.dosw.brigades_and_incidents.dto.ConsultaIncidentesRequest;
import edu.eci.dosw.brigades_and_incidents.dto.EvidenciaIncidenteDTO;
import edu.eci.dosw.brigades_and_incidents.dto.IncidenteDetalleDTO;
import edu.eci.dosw.brigades_and_incidents.dto.IncidenteResumenDTO;
import edu.eci.dosw.brigades_and_incidents.entity.AsignacionBrigadista;
import edu.eci.dosw.brigades_and_incidents.entity.Brigadista;
import edu.eci.dosw.brigades_and_incidents.entity.EvidenciaIncidente;
import edu.eci.dosw.brigades_and_incidents.entity.Incidente;
import edu.eci.dosw.brigades_and_incidents.repository.AsignacionBrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.BrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.EstadoIncidenteRepository;
import edu.eci.dosw.brigades_and_incidents.repository.EvidenciaIncidenteRepository;
import edu.eci.dosw.brigades_and_incidents.repository.IncidenteRepository;
import edu.eci.dosw.brigades_and_incidents.repository.IncidenteSpecification;
import edu.eci.dosw.brigades_and_incidents.repository.PrioridadIncidenteRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConsultaIncidentesService {

    private static final String ROL_COORDINADOR = "COORDINADOR";
    private static final String ROL_BRIGADISTA = "BRIGADISTA";
    private static final String ROL_AUDITOR = "AUDITOR";

    private final IncidenteRepository incidenteRepository;
    private final UsuarioClient usuarioClient;
    private final BrigadistaRepository brigadistaRepository;
    private final AsignacionBrigadistaRepository asignacionRepository;
    private final EvidenciaIncidenteRepository evidenciaRepository;

    private UbicacionClient ubicacionClient;
    private PrioridadIncidenteRepository prioridadRepository;
    private EstadoIncidenteRepository estadoRepository;

    public ConsultaIncidentesService(
            IncidenteRepository incidenteRepository,
            UsuarioClient usuarioClient,
            BrigadistaRepository brigadistaRepository,
            AsignacionBrigadistaRepository asignacionRepository,
            EvidenciaIncidenteRepository evidenciaRepository
    ) {
        this.incidenteRepository = incidenteRepository;
        this.usuarioClient = usuarioClient;
        this.brigadistaRepository = brigadistaRepository;
        this.asignacionRepository = asignacionRepository;
        this.evidenciaRepository = evidenciaRepository;
    }

    /**
     * Permite utilizar el cliente de ubicaciones cuando existe
     * una implementación disponible para el perfil activo.
     */
    @Autowired(required = false)
    public void setUbicacionClient(UbicacionClient ubicacionClient) {
        this.ubicacionClient = ubicacionClient;
    }

    /**
     * Permite validar los valores del catálogo de prioridades.
     */
    @Autowired(required = false)
    public void setPrioridadRepository(
            PrioridadIncidenteRepository prioridadRepository
    ) {
        this.prioridadRepository = prioridadRepository;
    }

    /**
     * Permite validar los valores del catálogo de estados.
     */
    @Autowired(required = false)
    public void setEstadoRepository(
            EstadoIncidenteRepository estadoRepository
    ) {
        this.estadoRepository = estadoRepository;
    }

    /**
     * Consulta los incidentes de un evento aplicando permisos,
     * filtros, ordenamiento y paginación.
     */
    @Transactional(readOnly = true)
    public Page<IncidenteResumenDTO> consultarIncidentes(
            String eventoId,
            ConsultaIncidentesRequest filtros,
            String usuarioId
    ) {
        validarIdentidad(usuarioId);

        if (eventoId == null || eventoId.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador del evento es obligatorio."
            );
        }

        UsuarioClient.UsuarioExterno usuario =
                usuarioClient.consultarUsuario(usuarioId);

        validarUsuarioExistenteYActivo(usuario);

        Set<String> roles = obtenerRoles(usuario);

        if (roles.contains(ROL_AUDITOR)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Los usuarios con rol auditor no pueden consultar incidentes."
            );
        }

        Specification<Incidente> specification =
                Specification.where(
                        IncidenteSpecification.porEvento(eventoId)
                ).and(
                        IncidenteSpecification.noEliminados()
                );

        if (roles.contains(ROL_COORDINADOR)) {
            // El coordinador puede consultar todos los incidentes
            // no eliminados del evento.
        } else if (roles.contains(ROL_BRIGADISTA)) {
            List<String> ubicacionesAsignadas =
                    obtenerUbicacionesAsignadas(usuarioId, eventoId);

            specification = specification.and(
                    IncidenteSpecification.visiblesParaBrigadista(
                            usuarioId,
                            ubicacionesAsignadas
                    )
            );
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El usuario no tiene permisos para consultar incidentes."
            );
        }

        if (filtros == null) {
            filtros = new ConsultaIncidentesRequest(
                    null, null, null, null, null, null, null
            );
        }

        // Aplicar únicamente los filtros de prioridad y estado válidos.
        if (filtros.prioridad() != null
                && !filtros.prioridad().isBlank()) {

            String prioridad = filtros.prioridad().trim();

            if (prioridadRepository == null
                    || prioridadRepository.findByNombreIgnoreCase(prioridad)
                    .isPresent()) {

                specification = specification.and(
                        IncidenteSpecification.porPrioridad(prioridad)
                );
            }
        }

        if (filtros.estado() != null
                && !filtros.estado().isBlank()) {

            String estado = filtros.estado().trim();

            if (estadoRepository == null
                    || estadoRepository.findByNombreIgnoreCase(estado)
                    .isPresent()) {

                specification = specification.and(
                        IncidenteSpecification.porEstado(estado)
                );
            }
        }

        // El filtro de ubicación se conserva.
        specification = specification.and(
                IncidenteSpecification.porUbicacion(
                        filtros.ubicacionId()
                )
        );

        Pageable pageable = construirPaginacion(filtros);

        // El nombre de la ubicación se obtiene desde MS02.
        // Por eso, este ordenamiento se realiza antes de paginar.
        if (esOrdenamientoPorEdificio(filtros.ordenarPor())) {
            return consultarOrdenadoPorEdificio(
                    specification,
                    pageable,
                    filtros.direccion()
            );
        }

        return incidenteRepository.findAll(specification, pageable)
                .map(this::convertirResumen);
    }

    /**
     * Consulta todos los incidentes filtrados, obtiene el nombre
     * de sus ubicaciones y realiza la paginación después de ordenar.
     */
    private Page<IncidenteResumenDTO> consultarOrdenadoPorEdificio(
            Specification<Incidente> specification,
            Pageable pageable,
            String direccion
    ) {
        if (ubicacionClient == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio de ubicaciones no está disponible."
            );
        }

        List<Incidente> incidentes =
                incidenteRepository.findAll(specification);

        Map<String, String> nombresUbicaciones = new HashMap<>();

        for (Incidente incidente : incidentes) {
            String tipoUbicacion =
                    incidente.getTipoUbicacion().getNombre();

            String clave = crearClaveUbicacion(
                    incidente.getUbicacionId(),
                    tipoUbicacion
            );

            if (!nombresUbicaciones.containsKey(clave)) {
                UbicacionClient.UbicacionExterna ubicacion =
                        ubicacionClient.consultarUbicacion(
                                incidente.getUbicacionId(),
                                tipoUbicacion
                        );

                nombresUbicaciones.put(
                        clave,
                        ubicacion == null || ubicacion.nombre() == null
                                ? ""
                                : ubicacion.nombre().trim()
                );
            }
        }

        Comparator<Incidente> comparador =
                Comparator.comparing(
                        incidente -> nombresUbicaciones.getOrDefault(
                                crearClaveUbicacion(
                                        incidente.getUbicacionId(),
                                        incidente.getTipoUbicacion().getNombre()
                                ),
                                ""
                        ),
                        String.CASE_INSENSITIVE_ORDER
                );

        // Si dos ubicaciones tienen el mismo nombre,
        // se utiliza la fecha de reporte como segundo criterio.
        comparador = comparador
                .thenComparing(
                        Incidente::getFechaReporte,
                        Comparator.nullsLast(Comparator.naturalOrder())
                )
                .thenComparing(Incidente::getId);

        if ("DESC".equalsIgnoreCase(direccion)) {
            comparador = comparador.reversed();
        }

        List<IncidenteResumenDTO> resultados = incidentes.stream()
                .sorted(comparador)
                .map(this::convertirResumen)
                .toList();

        int inicio = (int) pageable.getOffset();

        if (inicio >= resultados.size()) {
            return new PageImpl<>(
                    List.of(),
                    pageable,
                    resultados.size()
            );
        }

        int fin = Math.min(
                inicio + pageable.getPageSize(),
                resultados.size()
        );

        return new PageImpl<>(
                resultados.subList(inicio, fin),
                pageable,
                resultados.size()
        );
    }

    /**
     * Construye una clave única para identificar una ubicación
     * a partir de su identificador y su tipo.
     */
    private String crearClaveUbicacion(
            String ubicacionId,
            String tipoUbicacion
    ) {
        return String.valueOf(tipoUbicacion)
                + "|"
                + String.valueOf(ubicacionId);
    }

    /**
     * Comprueba si se solicitó ordenar por el nombre del edificio.
     */
    private boolean esOrdenamientoPorEdificio(String ordenarPor) {
        return ordenarPor != null
                && "edificio".equalsIgnoreCase(ordenarPor.trim());
    }

    /**
     * Consulta el detalle de un incidente y sus evidencias.
     * Solo permite el acceso a coordinadores y brigadistas autorizados.
     */
    @Transactional(readOnly = true)
    public IncidenteDetalleDTO consultarDetalleIncidente(
            String eventoId,
            UUID incidenteId,
            String usuarioId
    ) {
        validarIdentidad(usuarioId);

        if (eventoId == null || eventoId.isBlank()
                || incidenteId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El evento y el incidente son obligatorios."
            );
        }

        UsuarioClient.UsuarioExterno usuario =
                usuarioClient.consultarUsuario(usuarioId);

        validarUsuarioExistenteYActivo(usuario);

        Set<String> roles = obtenerRoles(usuario);

        if (roles.contains(ROL_AUDITOR)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Los usuarios con rol auditor no pueden consultar incidentes."
            );
        }

        Incidente incidente = incidenteRepository
                .findByIdAndEventoId(incidenteId, eventoId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "No se encontró el incidente en el evento indicado."
                        )
                );

        if (incidente.isEliminado()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El incidente no está disponible."
            );
        }

        if (roles.contains(ROL_COORDINADOR)) {
            // El coordinador puede consultar todos los incidentes
            // no eliminados del evento.
        } else if (roles.contains(ROL_BRIGADISTA)) {
            List<String> ubicacionesAsignadas =
                    obtenerUbicacionesAsignadas(usuarioId, eventoId);

            boolean puedeConsultar =
                    usuarioId.equals(incidente.getReportanteId())
                            || usuarioId.equals(
                                    incidente.getBrigadistaResponsableId()
                            )
                            || ubicacionesAsignadas.contains(
                                    incidente.getUbicacionId()
                            );

            if (!puedeConsultar) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No tiene permisos para consultar este incidente."
                );
            }
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El usuario no tiene permisos para consultar incidentes."
            );
        }

        List<EvidenciaIncidenteDTO> evidencias =
                evidenciaRepository.findByIncidenteId(incidenteId)
                        .stream()
                        .map(this::convertirEvidencia)
                        .toList();

        return new IncidenteDetalleDTO(
                incidente.getId(),
                incidente.getEventoId(),
                incidente.getNombreAfectado(),
                incidente.getUbicacionId(),
                incidente.getTipoUbicacion().getNombre(),
                incidente.getDescripcion(),
                incidente.getPrioridad().getNombre(),
                incidente.getEstado().getNombre(),
                incidente.getReportanteId(),
                incidente.getBrigadistaResponsableId(),
                incidente.getFechaReporte(),
                incidente.getFechaPrimeraAtencion(),
                incidente.getFechaResolucion(),
                incidente.getObservacionesResolucion(),
                incidente.getFechaCierre(),
                evidencias
        );
    }

    /**
     * Realiza la eliminación lógica de un incidente.
     * Solo los coordinadores pueden eliminar incidentes resueltos.
     */
    @Transactional
    public void eliminarIncidente(
            String eventoId,
            UUID incidenteId,
            String usuarioId
    ) {
        validarIdentidad(usuarioId);

        if (eventoId == null || eventoId.isBlank()
                || incidenteId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El evento y el incidente son obligatorios."
            );
        }

        UsuarioClient.UsuarioExterno usuario =
                usuarioClient.consultarUsuario(usuarioId);

        validarUsuarioExistenteYActivo(usuario);

        Set<String> roles = obtenerRoles(usuario);

        if (!roles.contains(ROL_COORDINADOR)
                || roles.contains(ROL_AUDITOR)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un coordinador puede eliminar incidentes."
            );
        }

        Incidente incidente = incidenteRepository
                .findByIdAndEventoIdAndEliminadoFalse(
                        incidenteId,
                        eventoId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "No se encontró el incidente o ya fue eliminado."
                        )
                );

        String estado = incidente.getEstado().getNombre();

        if (!"RESUELTO".equalsIgnoreCase(estado)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se pueden eliminar incidentes en estado RESUELTO."
            );
        }

        incidente.setEliminado(true);
        incidenteRepository.save(incidente);
    }

    /**
     * Convierte una evidencia en su DTO de respuesta.
     */
    private EvidenciaIncidenteDTO convertirEvidencia(
            EvidenciaIncidente evidencia
    ) {
        return new EvidenciaIncidenteDTO(
                evidencia.getId(),
                evidencia.getNombreArchivo(),
                evidencia.getTipoContenido(),
                evidencia.getReferenciaArchivo(),
                evidencia.getFechaCarga()
        );
    }

    /**
     * Comprueba que se haya recibido la identidad del usuario.
     */
    private void validarIdentidad(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "No se pudo identificar al usuario autenticado."
            );
        }
    }

    /**
     * Comprueba que el usuario exista y esté activo en MS01.
     */
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

    /**
     * Normaliza los roles recibidos desde MS01.
     */
    private Set<String> obtenerRoles(
            UsuarioClient.UsuarioExterno usuario
    ) {
        if (usuario.roles() == null) {
            return Set.of();
        }

        return usuario.roles().stream()
                .filter(rol -> rol != null && !rol.isBlank())
                .map(rol -> rol.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    /**
     * Comprueba que el brigadista esté registrado y activo en MS05.
     * También obtiene sus ubicaciones con asignación vigente en el evento.
     */
    private List<String> obtenerUbicacionesAsignadas(
            String brigadistaId,
            String eventoId
    ) {
        Brigadista brigadista = brigadistaRepository
                .findByUsuarioIdExterno(brigadistaId)
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
                    "El brigadista debe estar activo para consultar incidentes."
            );
        }

        return asignacionRepository
                .findByBrigadistaIdAndEventoId(brigadistaId, eventoId)
                .stream()
                .filter(this::tieneAsignacionVigente)
                .map(AsignacionBrigadista::getUbicacionId)
                .filter(ubicacionId ->
                        ubicacionId != null && !ubicacionId.isBlank()
                )
                .distinct()
                .toList();
    }

    /**
     * Comprueba que una asignación esté vigente.
     */
    private boolean tieneAsignacionVigente(
            AsignacionBrigadista asignacion
    ) {
        return asignacion.getEstado() != null
                && "VIGENTE".equalsIgnoreCase(
                        asignacion.getEstado().getNombre()
                );
    }

    /**
     * Construye los parámetros de paginación y ordenamiento.
     */
    private Pageable construirPaginacion(
            ConsultaIncidentesRequest filtros
    ) {
        int pagina = filtros.pagina() == null || filtros.pagina() < 0
                ? 0 : filtros.pagina();

        int tamanio = filtros.tamanio() == null || filtros.tamanio() <= 0
                ? 10 : Math.min(filtros.tamanio(), 100);

        // El ordenamiento por edificio se realiza en memoria,
        // después de consultar los nombres externos de las ubicaciones.
        if (esOrdenamientoPorEdificio(filtros.ordenarPor())) {
            return PageRequest.of(pagina, tamanio);
        }

        String campo = obtenerCampoOrden(filtros.ordenarPor());

        Sort.Direction direccion =
                "DESC".equalsIgnoreCase(filtros.direccion())
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        Sort sort = Sort.by(direccion, campo);

        return PageRequest.of(pagina, tamanio, sort);
    }

    /**
     * Determina el campo permitido para ordenar los incidentes.
     */
    private String obtenerCampoOrden(String ordenarPor) {
        if (ordenarPor == null || ordenarPor.isBlank()) {
            return "fechaReporte";
        }

        return switch (ordenarPor.trim().toLowerCase(Locale.ROOT)) {
            case "prioridad" -> "prioridad.nombre";
            case "estado" -> "estado.nombre";
            case "ubicacion" -> "ubicacionId";
            case "antiguedad", "fechareporte" -> "fechaReporte";
            default -> "fechaReporte";
        };
    }

    /**
     * Convierte una entidad de incidente en su representación resumida.
     */
    private IncidenteResumenDTO convertirResumen(Incidente incidente) {
        return new IncidenteResumenDTO(
                incidente.getId().toString(),
                incidente.getEventoId(),
                incidente.getUbicacionId(),
                incidente.getTipoUbicacion().getNombre(),
                incidente.getPrioridad().getNombre(),
                incidente.getEstado().getNombre(),
                incidente.getBrigadistaResponsableId(),
                incidente.getFechaReporte()
        );
    }
}
