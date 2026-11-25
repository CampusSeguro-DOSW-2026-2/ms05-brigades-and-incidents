package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.client.BrigadistaClient;
import edu.eci.dosw.brigades_and_incidents.client.BrigadistaClient.BrigadistaExterno;
import edu.eci.dosw.brigades_and_incidents.client.UbicacionClient;
import edu.eci.dosw.brigades_and_incidents.client.UbicacionClient.UbicacionExterna;
import edu.eci.dosw.brigades_and_incidents.dto.AutoasignacionEventoDTO;
import edu.eci.dosw.brigades_and_incidents.dto.AutoasignacionResponse;
import edu.eci.dosw.brigades_and_incidents.dto.BrigadistaGestionDTO;
import edu.eci.dosw.brigades_and_incidents.dto.EdificioEventoDTO;
import edu.eci.dosw.brigades_and_incidents.dto.EventoOperacionDTO;
import edu.eci.dosw.brigades_and_incidents.dto.GestionBrigadistasEventoResponse;
import edu.eci.dosw.brigades_and_incidents.entity.AsignacionBrigadista;
import edu.eci.dosw.brigades_and_incidents.entity.Brigadista;
import edu.eci.dosw.brigades_and_incidents.entity.ConfiguracionOperacionEvento;
import edu.eci.dosw.brigades_and_incidents.entity.EstadoAsignacion;
import edu.eci.dosw.brigades_and_incidents.entity.EstadoBrigadista;
import edu.eci.dosw.brigades_and_incidents.entity.Incidente;
import edu.eci.dosw.brigades_and_incidents.entity.TipoUbicacion;
import edu.eci.dosw.brigades_and_incidents.entity.ZonaEvento;
import edu.eci.dosw.brigades_and_incidents.event.AsignacionAuditEvent;
import edu.eci.dosw.brigades_and_incidents.exception.ConflictoOperacionException;
import edu.eci.dosw.brigades_and_incidents.exception.Ms01IntegrationException;
import edu.eci.dosw.brigades_and_incidents.exception.RecursoNoEncontradoException;
import edu.eci.dosw.brigades_and_incidents.repository.AsignacionBrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.BrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.ConfiguracionOperacionEventoRepository;
import edu.eci.dosw.brigades_and_incidents.repository.EstadoAsignacionRepository;
import edu.eci.dosw.brigades_and_incidents.repository.EstadoBrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.IncidenteRepository;
import edu.eci.dosw.brigades_and_incidents.repository.TipoUbicacionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OperacionBrigadistasService {

    private static final Logger log = LoggerFactory.getLogger(OperacionBrigadistasService.class);

    public static final String EVENTO_PROGRAMADO = "PROGRAMADO";
    public static final String EVENTO_EN_EJECUCION = "EN_EJECUCION";
    static final Set<String> ESTADOS_ABIERTOS = Set.of(EVENTO_PROGRAMADO, EVENTO_EN_EJECUCION);

    private static final String VIGENTE = "VIGENTE";
    private static final String RETIRADA = "RETIRADA";
    private static final String ACTIVO = "ACTIVO";
    private static final String INACTIVO = "INACTIVO";
    private static final String ROL_BRIGADISTA = "BRIGADISTA";
    private static final String INCIDENTE_EN_ATENCION = "EN_ATENCION";
    private static final Set<String> INCIDENTES_RESUELTOS = Set.of("RESUELTO", "CERRADO");

    private final ConfiguracionOperacionEventoRepository configuracionRepository;
    private final AsignacionBrigadistaRepository asignacionRepository;
    private final BrigadistaRepository brigadistaRepository;
    private final EstadoBrigadistaRepository estadoBrigadistaRepository;
    private final EstadoAsignacionRepository estadoAsignacionRepository;
    private final TipoUbicacionRepository tipoUbicacionRepository;
    private final IncidenteRepository incidenteRepository;
    private final BrigadistaClient brigadistaClient;
    private final UbicacionClient ubicacionClient;
    private final ApplicationEventPublisher eventPublisher;

    @SuppressWarnings("java:S107")
    public OperacionBrigadistasService(
            ConfiguracionOperacionEventoRepository configuracionRepository,
            AsignacionBrigadistaRepository asignacionRepository,
            BrigadistaRepository brigadistaRepository,
            EstadoBrigadistaRepository estadoBrigadistaRepository,
            EstadoAsignacionRepository estadoAsignacionRepository,
            TipoUbicacionRepository tipoUbicacionRepository,
            IncidenteRepository incidenteRepository,
            BrigadistaClient brigadistaClient,
            UbicacionClient ubicacionClient,
            ApplicationEventPublisher eventPublisher
    ) {
        this.configuracionRepository = configuracionRepository;
        this.asignacionRepository = asignacionRepository;
        this.brigadistaRepository = brigadistaRepository;
        this.estadoBrigadistaRepository = estadoBrigadistaRepository;
        this.estadoAsignacionRepository = estadoAsignacionRepository;
        this.tipoUbicacionRepository = tipoUbicacionRepository;
        this.incidenteRepository = incidenteRepository;
        this.brigadistaClient = brigadistaClient;
        this.ubicacionClient = ubicacionClient;
        this.eventPublisher = eventPublisher;
    }

    // Eventos abiertos

    /**
     * Eventos programados o en ejecución que puede gestionar quien consulta (el coordinador, los reales; el
     * organizador, los simulacros), con sus edificios y cuántos brigadistas tiene cada uno.
     */
    @Transactional(readOnly = true)
    public List<EventoOperacionDTO> consultarEventosAbiertos(AlcanceGestion alcance) {
        Map<String, String> cacheNombres = new HashMap<>();
        return eventosAbiertos().stream()
                .filter(configuracion -> alcance.permite(configuracion.isSimulacro()))
                .map(configuracion -> convertirEvento(configuracion, alcance, cacheNombres))
                .toList();
    }

    // Panel de gestión (coordinador)

    @Transactional
    public GestionBrigadistasEventoResponse consultarGestion(String eventoId, AlcanceGestion alcance) {
        ConfiguracionOperacionEvento configuracion = buscarEvento(eventoId);
        verificarAlcance(configuracion, alcance);

        List<BrigadistaExterno> externos = listarBrigadistasMs01();
        sincronizarBrigadistas(externos);

        Map<String, String> nombres = new HashMap<>();
        for (BrigadistaExterno externo : externos) {
            if (esBrigadista(externo)) {
                nombres.put(externo.id(), externo.nombre().trim());
            }
        }

        List<AsignacionBrigadista> vigentes = asignacionRepository
                .findByEventoIdAndEstado_Nombre(eventoId, VIGENTE);

        Map<String, AsignacionBrigadista> asignacionPorBrigadista = new LinkedHashMap<>();
        for (AsignacionBrigadista asignacion : vigentes) {
            asignacionPorBrigadista.putIfAbsent(asignacion.getBrigadistaId(), asignacion);
        }

        // Brigadistas de MS01 y los que, aunque ya no estén en MS01, tienen asignación vigente en el evento.
        List<String> ids = new ArrayList<>(nombres.keySet());
        asignacionPorBrigadista.keySet().stream()
                .filter(id -> !nombres.containsKey(id))
                .forEach(ids::add);

        Map<String, String> estados = brigadistaRepository.findByUsuarioIdExternoIn(ids)
                .stream()
                .collect(Collectors.toMap(
                        Brigadista::getUsuarioIdExterno,
                        brigadista -> brigadista.getEstado().getNombre(),
                        (primero, segundo) -> primero
                ));

        List<Incidente> incidentes = incidenteRepository.findByEventoId(eventoId)
                .stream()
                .filter(incidente -> !incidente.isEliminado())
                .filter(incidente -> incidente.getBrigadistaResponsableId() != null)
                .toList();

        Map<String, String> cacheNombres = new HashMap<>();

        List<BrigadistaGestionDTO> brigadistas = ids.stream()
                .map(id -> {
                    AsignacionBrigadista asignacion = asignacionPorBrigadista.get(id);
                    String ubicacionId = asignacion == null ? null : asignacion.getUbicacionId();
                    return new BrigadistaGestionDTO(
                            id,
                            nombres.getOrDefault(id, id),
                            estados.getOrDefault(id, INACTIVO),
                            asignacion == null ? null : asignacion.getIdAsignacion().toString(),
                            ubicacionId,
                            ubicacionId == null ? null : nombreUbicacion(configuracion, ubicacionId, cacheNombres),
                            contarIncidentes(incidentes, id, Set.of(INCIDENTE_EN_ATENCION)),
                            contarIncidentes(incidentes, id, INCIDENTES_RESUELTOS)
                    );
                })
                .sorted(ORDEN_PANEL)
                .toList();

        return new GestionBrigadistasEventoResponse(
                convertirEvento(configuracion, vigentes, alcance, cacheNombres),
                brigadistas
        );
    }

    /**
     * Cambia (o da por primera vez) el edificio de un brigadista: el coordinador en eventos reales y el organizador
     * en simulacros.
     */
    @Transactional
    public BrigadistaGestionDTO cambiarAsignacion(
            String eventoId,
            String brigadistaId,
            String ubicacionId,
            String actorId,
            AlcanceGestion alcance
    ) {
        validarTexto(actorId, "No se pudo identificar al usuario que realiza la operación.");
        validarTexto(brigadistaId, "El brigadista es obligatorio.");

        ConfiguracionOperacionEvento configuracion = buscarEvento(eventoId);
        verificarAlcance(configuracion, alcance);
        validarEventoAbierto(configuracion);

        Brigadista brigadista = brigadistaRepository.findByUsuarioIdExterno(brigadistaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el brigadista indicado."
                ));

        if (!ACTIVO.equals(brigadista.getEstado().getNombre())) {
            throw new ConflictoOperacionException(
                    "No se pueden hacer asignaciones a brigadistas inactivos."
            );
        }

        Map<String, String> cacheNombres = new HashMap<>();
        ZonaEvento zona = buscarZona(configuracion, ubicacionId);
        AsignacionBrigadista asignacion = asignarEnEvento(configuracion, brigadistaId, zona, actorId, cacheNombres);

        return new BrigadistaGestionDTO(
                brigadistaId,
                brigadistaId,
                ACTIVO,
                asignacion.getIdAsignacion().toString(),
                zona.getZonaId(),
                resolverNombreZona(zona, cacheNombres),
                0,
                0
        );
    }

    // Auto asignación (brigadista)

    @Transactional
    public AutoasignacionResponse consultarAutoasignacion(String brigadistaId) {
        validarTexto(brigadistaId, "No se pudo identificar al brigadista.");

        Brigadista brigadista = obtenerOCrearBrigadista(brigadistaId);
        Map<String, String> cacheNombres = new HashMap<>();

        List<AutoasignacionEventoDTO> eventos = eventosAbiertos().stream()
                .map(configuracion -> {
                    Optional<AsignacionBrigadista> propia = asignacionVigente(
                            configuracion.getEventoId(), brigadistaId);
                    return new AutoasignacionEventoDTO(
                            convertirEvento(configuracion, cacheNombres),
                            propia.map(a -> a.getIdAsignacion().toString()).orElse(null),
                            propia.map(AsignacionBrigadista::getUbicacionId).orElse(null),
                            propia.map(a -> nombreUbicacion(configuracion, a.getUbicacionId(), cacheNombres))
                                    .orElse(null)
                    );
                })
                .toList();

        return new AutoasignacionResponse(
                brigadistaId,
                brigadista.getEstado().getNombre(),
                eventos
        );
    }

    /** El brigadista elige su edificio o se cambia a otro. Debe estar activo. */
    @Transactional
    public AutoasignacionEventoDTO autoasignarse(
            String eventoId,
            String brigadistaId,
            String ubicacionId
    ) {
        validarTexto(brigadistaId, "No se pudo identificar al brigadista.");

        ConfiguracionOperacionEvento configuracion = buscarEvento(eventoId);
        validarEventoAbierto(configuracion);

        Brigadista brigadista = obtenerOCrearBrigadista(brigadistaId);
        if (!ACTIVO.equals(brigadista.getEstado().getNombre())) {
            throw new ConflictoOperacionException(
                    "Para asignarte a un edificio primero debes activarte."
            );
        }

        Map<String, String> cacheNombres = new HashMap<>();
        ZonaEvento zona = buscarZona(configuracion, ubicacionId);
        AsignacionBrigadista asignacion = asignarEnEvento(configuracion, brigadistaId, zona, brigadistaId, cacheNombres);

        return new AutoasignacionEventoDTO(
                convertirEvento(configuracion, cacheNombres),
                asignacion.getIdAsignacion().toString(),
                zona.getZonaId(),
                resolverNombreZona(zona, cacheNombres)
        );
    }

    /** El brigadista quita su asignación del evento, salvo que sea el único en su edificio. */
    @Transactional
    public AutoasignacionEventoDTO retirarAutoasignacion(
            String eventoId,
            String brigadistaId
    ) {
        validarTexto(brigadistaId, "No se pudo identificar al brigadista.");

        ConfiguracionOperacionEvento configuracion = buscarEvento(eventoId);
        validarEventoAbierto(configuracion);

        List<AsignacionBrigadista> vigentes = asignacionRepository
                .findByEventoIdAndBrigadistaIdAndEstado_Nombre(eventoId, brigadistaId, VIGENTE);

        if (vigentes.isEmpty()) {
            throw new ConflictoOperacionException(
                    "No tienes un edificio asignado en este evento."
            );
        }

        Map<String, String> cacheNombres = new HashMap<>();
        EstadoAsignacion retirada = buscarEstadoAsignacion(RETIRADA);
        for (AsignacionBrigadista asignacion : vigentes) {
            validarNoDejarEdificioSinBrigadistas(configuracion, asignacion, cacheNombres);
            asignacion.setEstado(retirada);
            AsignacionBrigadista guardada = asignacionRepository.save(asignacion);
            publicarAuditoria(guardada, "RETIRO", brigadistaId);
        }

        return new AutoasignacionEventoDTO(
                convertirEvento(configuracion, cacheNombres),
                null,
                null,
                null
        );
    }

    /**
     * Deja al brigadista con una sola asignación vigente en el evento, en el edificio indicado. Si ya estaba en
     * otro edificio, se cambia la asignación existente (validando que ese edificio no quede sin brigadistas).
     */
    private AsignacionBrigadista asignarEnEvento(
            ConfiguracionOperacionEvento configuracion,
            String brigadistaId,
            ZonaEvento zona,
            String actorId,
            Map<String, String> cacheNombres
    ) {
        String eventoId = configuracion.getEventoId();
        List<AsignacionBrigadista> vigentes = asignacionRepository
                .findByEventoIdAndBrigadistaIdAndEstado_Nombre(eventoId, brigadistaId, VIGENTE);

        boolean yaEnEdificio = vigentes.stream()
                .anyMatch(asignacion -> zona.getZonaId().equals(asignacion.getUbicacionId()));

        if (yaEnEdificio) {
            throw new ConflictoOperacionException(
                    "El brigadista ya está asignado a " + resolverNombreZona(zona, cacheNombres) + "."
            );
        }

        TipoUbicacion tipo = buscarTipo(zona.getTipoUbicacion());

        if (vigentes.isEmpty()) {
            AsignacionBrigadista nueva = new AsignacionBrigadista();
            nueva.setBrigadistaId(brigadistaId);
            nueva.setEventoId(eventoId);
            nueva.setUbicacionId(zona.getZonaId());
            nueva.setTipoUbicacion(tipo);
            nueva.setEstado(buscarEstadoAsignacion(VIGENTE));

            AsignacionBrigadista guardada = asignacionRepository.save(nueva);
            publicarAuditoria(guardada, "CREACION", actorId);
            return guardada;
        }

        for (AsignacionBrigadista anterior : vigentes) {
            validarNoDejarEdificioSinBrigadistas(configuracion, anterior, cacheNombres);
        }

        // Una sola asignación por evento: se conserva la primera y se retiran las sobrantes (datos antiguos).
        AsignacionBrigadista principal = vigentes.get(0);
        EstadoAsignacion retirada = null;
        for (AsignacionBrigadista sobrante : vigentes.subList(1, vigentes.size())) {
            if (retirada == null) {
                retirada = buscarEstadoAsignacion(RETIRADA);
            }
            sobrante.setEstado(retirada);
            publicarAuditoria(asignacionRepository.save(sobrante), "RETIRO", actorId);
        }

        principal.setUbicacionId(zona.getZonaId());
        principal.setTipoUbicacion(tipo);

        AsignacionBrigadista guardada = asignacionRepository.save(principal);
        publicarAuditoria(guardada, "MODIFICACION", actorId);
        return guardada;
    }

    /** Un edificio del evento no puede quedar sin brigadistas: el único asignado no puede irse. */
    private void validarNoDejarEdificioSinBrigadistas(
            ConfiguracionOperacionEvento configuracion,
            AsignacionBrigadista asignacion,
            Map<String, String> cacheNombres
    ) {
        long otros = asignacionRepository.countByEventoIdAndUbicacionIdAndEstado_NombreAndIdAsignacionNot(
                asignacion.getEventoId(),
                asignacion.getUbicacionId(),
                VIGENTE,
                asignacion.getIdAsignacion()
        );

        if (otros == 0) {
            throw new ConflictoOperacionException(
                    "No es posible: el brigadista es el único asignado a "
                    + nombreUbicacion(configuracion, asignacion.getUbicacionId(), cacheNombres)
                    + " y el edificio no puede quedar sin brigadistas."
            );
        }
    }

    /** El coordinador solo actúa sobre eventos reales y el organizador solo sobre simulacros. */
    private void verificarAlcance(ConfiguracionOperacionEvento configuracion, AlcanceGestion alcance) {
        if (alcance == null || !alcance.permite(configuracion.isSimulacro())) {
            throw new AccessDeniedException(AlcanceGestion.mensajeNoPermitido(configuracion.isSimulacro()));
        }
    }

    private void validarEventoAbierto(ConfiguracionOperacionEvento configuracion) {
        if (!ESTADOS_ABIERTOS.contains(configuracion.getEstadoEvento())) {
            throw new ConflictoOperacionException(
                    "El evento no está programado ni en ejecución; no admite cambios de asignación."
            );
        }
    }

    private ZonaEvento buscarZona(ConfiguracionOperacionEvento configuracion, String ubicacionId) {
        validarTexto(ubicacionId, "El edificio es obligatorio.");
        return configuracion.buscarZona(ubicacionId.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El edificio indicado no hace parte del evento."
                ));
    }
   

    /** Orden del panel: activos con edificio (por edificio y nombre), activos sin edificio, inactivos. */
    static final Comparator<BrigadistaGestionDTO> ORDEN_PANEL = Comparator
            .comparing((BrigadistaGestionDTO b) -> !ACTIVO.equals(b.estado()))
            .thenComparing(b -> b.nombreUbicacion() == null)
            .thenComparing(
                    (BrigadistaGestionDTO b) -> b.nombreUbicacion() == null ? "" : b.nombreUbicacion(),
                    String.CASE_INSENSITIVE_ORDER
            )
            .thenComparing(BrigadistaGestionDTO::nombre, String.CASE_INSENSITIVE_ORDER);

    private List<ConfiguracionOperacionEvento> eventosAbiertos() {
        return configuracionRepository.findByEstadoEventoIn(ESTADOS_ABIERTOS)
                .stream()
                .sorted(Comparator.comparing(
                        ConfiguracionOperacionEvento::getNombreVisible,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();
    }

    /** Evento para la vista del brigadista: nunca permite reasignar. */
    private EventoOperacionDTO convertirEvento(
            ConfiguracionOperacionEvento configuracion,
            Map<String, String> cacheNombres
    ) {
        return convertirEvento(configuracion, (AlcanceGestion) null, cacheNombres);
    }

    private EventoOperacionDTO convertirEvento(
            ConfiguracionOperacionEvento configuracion,
            AlcanceGestion alcance,
            Map<String, String> cacheNombres
    ) {
        return convertirEvento(
                configuracion,
                asignacionRepository.findByEventoIdAndEstado_Nombre(configuracion.getEventoId(), VIGENTE),
                alcance,
                cacheNombres
        );
    }

    private EventoOperacionDTO convertirEvento(
            ConfiguracionOperacionEvento configuracion,
            List<AsignacionBrigadista> vigentes,
            AlcanceGestion alcance,
            Map<String, String> cacheNombres
    ) {
        Map<String, Long> porUbicacion = vigentes.stream()
                .collect(Collectors.groupingBy(AsignacionBrigadista::getUbicacionId, Collectors.counting()));

        List<EdificioEventoDTO> edificios = configuracion.getZonas().stream()
                .map(zona -> new EdificioEventoDTO(
                        zona.getZonaId(),
                        resolverNombreZona(zona, cacheNombres),
                        zona.getTipoUbicacion(),
                        porUbicacion.getOrDefault(zona.getZonaId(), 0L)
                ))
                .sorted(Comparator.comparing(EdificioEventoDTO::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();

        boolean abierto = ESTADOS_ABIERTOS.contains(configuracion.getEstadoEvento());

        return new EventoOperacionDTO(
                configuracion.getEventoId(),
                configuracion.getNombreVisible(),
                configuracion.isSimulacro(),
                configuracion.getEstadoEvento(),
                abierto && alcance != null && alcance.permite(configuracion.isSimulacro()),
                edificios
        );
    }

    private Optional<AsignacionBrigadista> asignacionVigente(String eventoId, String brigadistaId) {
        return asignacionRepository
                .findByEventoIdAndBrigadistaIdAndEstado_Nombre(eventoId, brigadistaId, VIGENTE)
                .stream()
                .findFirst();
    }

    private String nombreUbicacion(
            ConfiguracionOperacionEvento configuracion,
            String ubicacionId,
            Map<String, String> cacheNombres
    ) {
        return configuracion.buscarZona(ubicacionId)
                .map(zona -> resolverNombreZona(zona, cacheNombres))
                .orElse(ubicacionId);
    }

    /**
     * Resuelve el nombre visible de una zona del evento: si MS06 ya envió el nombre, lo usa directamente;
     * si llegó vacío o nulo, consulta MS02 como respaldo (con caché por petición) y, si MS02 no responde
     * o no lo encuentra, devuelve el id como último recurso.
     */
    private String resolverNombreZona(ZonaEvento zona, Map<String, String> cacheNombres) {
        if (zona.getNombre() != null && !zona.getNombre().isBlank()) {
            return zona.getNombre().trim();
        }
        String clave = zona.getTipoUbicacion() + "|" + zona.getZonaId();
        return cacheNombres.computeIfAbsent(clave, k -> consultarNombreEnMs02(zona));
    }

    private String consultarNombreEnMs02(ZonaEvento zona) {
        if (ubicacionClient == null) {
            return zona.getZonaId();
        }
        try {
            UbicacionExterna externa = ubicacionClient.consultarUbicacion(
                    zona.getZonaId(),
                    zona.getTipoUbicacion()
            );
            if (externa != null && externa.nombre() != null && !externa.nombre().isBlank()) {
                return externa.nombre().trim();
            }
        } catch (RuntimeException exception) {
            log.warn("No fue posible resolver el nombre de la ubicación {} ({}) en MS02; se usa el id.",
                    zona.getZonaId(), zona.getTipoUbicacion());
        }
        return zona.getZonaId();
    }

    private long contarIncidentes(List<Incidente> incidentes, String brigadistaId, Set<String> estados) {
        return incidentes.stream()
                .filter(incidente -> brigadistaId.equals(incidente.getBrigadistaResponsableId()))
                .filter(incidente -> incidente.getEstado() != null
                        && estados.contains(incidente.getEstado().getNombre()))
                .count();
    }

    private ConfiguracionOperacionEvento buscarEvento(String eventoId) {
        validarTexto(eventoId, "El identificador del evento es obligatorio.");
        return configuracionRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El evento no está registrado en el módulo de brigadistas."
                ));
    }

    /** El brigadista se registra localmente (inactivo) la primera vez que entra, para poder activarse. */
    private Brigadista obtenerOCrearBrigadista(String brigadistaId) {
        return brigadistaRepository.findByUsuarioIdExterno(brigadistaId)
                .orElseGet(() -> brigadistaRepository.save(
                        new Brigadista(brigadistaId, buscarEstadoBrigadista(INACTIVO))
                ));
    }

    private List<BrigadistaExterno> listarBrigadistasMs01() {
        try {
            List<BrigadistaExterno> externos = brigadistaClient.listarBrigadistas();
            return externos == null ? List.of() : externos;
        } catch (Ms01IntegrationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No fue posible consultar los brigadistas en MS01",
                    exception
            );
        }
    }

    private void sincronizarBrigadistas(List<BrigadistaExterno> externos) {
        List<String> ids = externos.stream()
                .filter(this::esBrigadista)
                .map(BrigadistaExterno::id)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return;
        }

        Set<String> existentes = brigadistaRepository.findByUsuarioIdExternoIn(ids)
                .stream()
                .map(Brigadista::getUsuarioIdExterno)
                .collect(Collectors.toSet());

        List<String> nuevos = ids.stream()
                .filter(id -> !existentes.contains(id))
                .toList();

        if (nuevos.isEmpty()) {
            return;
        }

        EstadoBrigadista inactivo = buscarEstadoBrigadista(INACTIVO);
        brigadistaRepository.saveAll(nuevos.stream()
                .map(id -> new Brigadista(id, inactivo))
                .toList());
    }

    private boolean esBrigadista(BrigadistaExterno externo) {
        return externo != null
                && externo.id() != null
                && !externo.id().isBlank()
                && externo.nombre() != null
                && !externo.nombre().isBlank()
                && ROL_BRIGADISTA.equalsIgnoreCase(externo.rol());
    }

    private EstadoBrigadista buscarEstadoBrigadista(String nombre) {
        return estadoBrigadistaRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalStateException("No existe el estado " + nombre));
    }

    private EstadoAsignacion buscarEstadoAsignacion(String nombre) {
        return estadoAsignacionRepository.findByNombre(nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No está registrado el estado: " + nombre
                ));
    }

    private TipoUbicacion buscarTipo(String nombre) {
        String normalizado = Objects.requireNonNullElse(nombre, ZonaEvento.TIPO_POR_DEFECTO)
                .trim()
                .toUpperCase(Locale.ROOT);
        return tipoUbicacionRepository.findByNombre(normalizado)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No está registrado el tipo de ubicación: " + nombre
                ));
    }

    private static void validarTexto(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    private void publicarAuditoria(AsignacionBrigadista asignacion, String accion, String actorId) {
        eventPublisher.publishEvent(new AsignacionAuditEvent(
                asignacion.getIdAsignacion(),
                accion,
                actorId,
                asignacion.getBrigadistaId(),
                asignacion.getEventoId(),
                LocalDateTime.now()
        ));
    }
}