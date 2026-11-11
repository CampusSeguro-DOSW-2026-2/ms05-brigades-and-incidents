
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.client.UbicacionClient;
import edu.eci.dosw.brigades_and_incidents.dto.*;
import edu.eci.dosw.brigades_and_incidents.entity.*;
import edu.eci.dosw.brigades_and_incidents.event.AsignacionAuditEvent;
import edu.eci.dosw.brigades_and_incidents.exception.*;
import edu.eci.dosw.brigades_and_incidents.repository.*;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AsignacionBrigadistasService {

    private static final String VIGENTE = "VIGENTE";
    private static final String RETIRADA = "RETIRADA";
    private static final String RESUELTO = "RESUELTO";
    private static final String CERRADO = "CERRADO";

    private final AsignacionBrigadistaRepository asignacionRepository;
    private final EstadoAsignacionRepository estadoRepository;
    private final TipoUbicacionRepository tipoRepository;
    private final IncidenteRepository incidenteRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UbicacionClient ubicacionClient;

    public AsignacionBrigadistasService(
            AsignacionBrigadistaRepository asignacionRepository,
            EstadoAsignacionRepository estadoRepository,
            TipoUbicacionRepository tipoRepository,
            IncidenteRepository incidenteRepository,
            ApplicationEventPublisher eventPublisher,
            UbicacionClient ubicacionClient) {

        this.asignacionRepository = asignacionRepository;
        this.estadoRepository = estadoRepository;
        this.tipoRepository = tipoRepository;
        this.incidenteRepository = incidenteRepository;
        this.eventPublisher = eventPublisher;
        this.ubicacionClient = ubicacionClient;
    }

    @Transactional
    public AsignacionResponse crearAsignacion(
            CrearAsignacionRequest request,
            String actorId) {

        validarIdentificadorActor(actorId);

        validarDatos(
                request.brigadistaId(),
                request.eventoId(),
                request.ubicacionId(),
                request.tipoUbicacion()
        );

        TipoUbicacion tipo = buscarTipo(request.tipoUbicacion());

        validarUbicacionExterna(
                request.ubicacionId(),
                tipo.getNombre()
        );

        validarDuplicado(
                request.brigadistaId(),
                request.eventoId(),
                request.ubicacionId(),
                tipo.getNombre()
        );

        EstadoAsignacion estado = buscarEstado(VIGENTE);

        AsignacionBrigadista asignacion = new AsignacionBrigadista();
        asignacion.setBrigadistaId(request.brigadistaId());
        asignacion.setEventoId(request.eventoId());
        asignacion.setUbicacionId(request.ubicacionId());
        asignacion.setTipoUbicacion(tipo);
        asignacion.setEstado(estado);
        asignacion.setObservaciones(request.observaciones());

        AsignacionBrigadista guardada =
                asignacionRepository.save(asignacion);

        publicarAuditoria(guardada, "CREACION", actorId);

        return convertirRespuesta(guardada);
    }

    @Transactional
    public AsignacionResponse modificarAsignacion(
            UUID idAsignacion,
            ModificarAsignacionRequest request,
            String actorId) {

        validarIdentificadorActor(actorId);

        AsignacionBrigadista asignacion =
                buscarAsignacion(idAsignacion);

        validarVigencia(asignacion);

        validarDatos(
                asignacion.getBrigadistaId(),
                asignacion.getEventoId(),
                request.ubicacionId(),
                request.tipoUbicacion()
        );

        TipoUbicacion tipo = buscarTipo(request.tipoUbicacion());

        validarUbicacionExterna(
                request.ubicacionId(),
                tipo.getNombre()
        );

        boolean cambiaUbicacion =
                !asignacion.getUbicacionId().equals(request.ubicacionId())
                || !asignacion.getTipoUbicacion().getNombre()
                        .equalsIgnoreCase(tipo.getNombre());

        if (cambiaUbicacion) {
            validarNoDejarZonaSinBrigadista(asignacion);
        }

        boolean duplicada = asignacionRepository
                .existsByBrigadistaIdAndEventoIdAndUbicacionIdAndTipoUbicacion_NombreAndEstado_NombreAndIdAsignacionNot(
                        asignacion.getBrigadistaId(),
                        asignacion.getEventoId(),
                        request.ubicacionId(),
                        tipo.getNombre(),
                        VIGENTE,
                        idAsignacion
                );

        if (duplicada) {
            throw new ConflictoOperacionException(
                    "Ya existe otra asignación vigente idéntica."
            );
        }

        asignacion.setUbicacionId(request.ubicacionId());
        asignacion.setTipoUbicacion(tipo);
        asignacion.setObservaciones(request.observaciones());

        AsignacionBrigadista actualizada =
                asignacionRepository.save(asignacion);

        publicarAuditoria(actualizada, "MODIFICACION", actorId);

        return convertirRespuesta(actualizada);
    }

    @Transactional
    public AsignacionResponse retirarAsignacion(
            UUID idAsignacion,
            String actorId) {

        validarIdentificadorActor(actorId);

        AsignacionBrigadista asignacion =
                buscarAsignacion(idAsignacion);

        validarVigencia(asignacion);

        validarNoDejarZonaSinBrigadista(asignacion);

        asignacion.setEstado(buscarEstado(RETIRADA));

        AsignacionBrigadista retirada =
                asignacionRepository.save(asignacion);

        publicarAuditoria(retirada, "RETIRO", actorId);

        return convertirRespuesta(retirada);
    }

    @Transactional
    public List<AsignacionResponse> crearAsignacionesPorDefecto(
            List<AsignacionPorDefectoRequest> solicitudes,
            String actorId) {

        validarIdentificadorActor(actorId);

        if (solicitudes == null || solicitudes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe proporcionar al menos una asignación."
            );
        }

        return solicitudes.stream()
                .map(solicitud -> crearAsignacion(
                        new CrearAsignacionRequest(
                                solicitud.brigadistaId(),
                                solicitud.eventoId(),
                                solicitud.ubicacionId(),
                                solicitud.tipoUbicacion(),
                                solicitud.observaciones()
                        ),
                        actorId
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public AsignacionResponse consultarAsignacion(UUID idAsignacion) {

        return convertirRespuesta(buscarAsignacion(idAsignacion));
    }

    @Transactional(readOnly = true)
    public List<AsignacionResponse> consultarPorEvento(String eventoId) {

        if (eventoId == null || eventoId.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador del evento es obligatorio."
            );
        }

        return asignacionRepository.findByEventoId(eventoId)
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AsignacionResponse> consultarPorBrigadista(
            String brigadistaId) {

        if (brigadistaId == null || brigadistaId.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador del brigadista es obligatorio."
            );
        }

        return asignacionRepository.findByBrigadistaId(brigadistaId)
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    /**
     * Comprueba que la ubicación exista, esté activa y corresponda
     * al tipo solicitado en el microservicio MS02.
     */
    private void validarUbicacionExterna(
            String ubicacionId,
            String tipoUbicacion) {

        UbicacionClient.UbicacionExterna ubicacion =
                ubicacionClient.consultarUbicacion(
                        ubicacionId,
                        tipoUbicacion
                );

        if (ubicacion == null) {
            throw new RecursoNoEncontradoException(
                    "No existe una ubicación con identificador "
                    + ubicacionId + " y tipo " + tipoUbicacion + " en MS02."
            );
        }

        if (!ubicacion.activa()) {
            throw new ConflictoOperacionException(
                    "No se puede asignar un brigadista a una ubicación inactiva."
            );
        }

        if (!ubicacionId.equals(ubicacion.id())
                || !tipoUbicacion.equalsIgnoreCase(ubicacion.tipo())) {

            throw new ConflictoOperacionException(
                    "La ubicación consultada no corresponde con los datos solicitados."
            );
        }
    }

    /**
     * Impide retirar o cambiar de ubicación a un brigadista
     * cuando existen incidentes pendientes y es el único
     * brigadista vigente asignado a esa ubicación del evento.
     */
    private void validarNoDejarZonaSinBrigadista(
            AsignacionBrigadista asignacion) {

        List<Incidente> incidentesPendientes =
                incidenteRepository.findByEventoIdAndUbicacionId(
                        asignacion.getEventoId(),
                        asignacion.getUbicacionId()
                ).stream()
                        .filter(incidente -> {
                            String estado =
                                    incidente.getEstado().getNombre();

                            return !RESUELTO.equals(estado)
                                    && !CERRADO.equals(estado);
                        })
                        .toList();

        if (incidentesPendientes.isEmpty()) {
            return;
        }

        boolean existeOtroBrigadista =
                asignacionRepository
                        .findByEventoIdAndEstado_Nombre(
                                asignacion.getEventoId(),
                                VIGENTE
                        )
                        .stream()
                        .anyMatch(otra ->
                                !otra.getIdAsignacion().equals(
                                        asignacion.getIdAsignacion())
                                && otra.getUbicacionId().equals(
                                        asignacion.getUbicacionId())
                                && otra.getTipoUbicacion().getNombre()
                                        .equals(
                                                asignacion.getTipoUbicacion()
                                                        .getNombre())
                        );

        if (!existeOtroBrigadista) {
            throw new ConflictoOperacionException(
                    "No se puede retirar o modificar esta asignación "
                    + "porque existen incidentes sin resolver y el "
                    + "brigadista es el único asignado a esta ubicación."
            );
        }
    }

    private void validarDatos(
            String brigadistaId,
            String eventoId,
            String ubicacionId,
            String tipoUbicacion) {

        if (brigadistaId == null || brigadistaId.isBlank()) {
            throw new IllegalArgumentException(
                    "El brigadista es obligatorio."
            );
        }

        if (eventoId == null || eventoId.isBlank()) {
            throw new IllegalArgumentException(
                    "El evento es obligatorio."
            );
        }

        if (ubicacionId == null || ubicacionId.isBlank()) {
            throw new IllegalArgumentException(
                    "La ubicación es obligatoria."
            );
        }

        if (tipoUbicacion == null || tipoUbicacion.isBlank()) {
            throw new IllegalArgumentException(
                    "El tipo de ubicación es obligatorio."
            );
        }
    }

    private void validarIdentificadorActor(String actorId) {

        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException(
                    "No se pudo identificar al usuario que realiza la operación."
            );
        }
    }

    private void validarDuplicado(
            String brigadistaId,
            String eventoId,
            String ubicacionId,
            String tipoUbicacion) {

        boolean existe = asignacionRepository
                .existsByBrigadistaIdAndEventoIdAndUbicacionIdAndTipoUbicacion_NombreAndEstado_Nombre(
                        brigadistaId,
                        eventoId,
                        ubicacionId,
                        tipoUbicacion,
                        VIGENTE
                );

        if (existe) {
            throw new ConflictoOperacionException(
                    "Ya existe una asignación vigente para este brigadista, ubicación y evento."
            );
        }
    }

    private void validarVigencia(AsignacionBrigadista asignacion) {

        if (!VIGENTE.equals(asignacion.getEstado().getNombre())) {
            throw new ConflictoOperacionException(
                    "La asignación no está vigente y no puede modificarse."
            );
        }
    }

    private AsignacionBrigadista buscarAsignacion(UUID id) {

        return asignacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la asignación con identificador: " + id
                ));
    }

    private EstadoAsignacion buscarEstado(String nombre) {

        return estadoRepository.findByNombre(nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No está registrado el estado: " + nombre
                ));
    }

    private TipoUbicacion buscarTipo(String nombre) {

        String tipoNormalizado = nombre.trim().toUpperCase();

        return tipoRepository.findByNombre(tipoNormalizado)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No está registrado el tipo de ubicación: " + nombre
                ));
    }

    private AsignacionResponse convertirRespuesta(
            AsignacionBrigadista asignacion) {

        return new AsignacionResponse(
                asignacion.getIdAsignacion(),
                asignacion.getBrigadistaId(),
                asignacion.getEventoId(),
                asignacion.getUbicacionId(),
                asignacion.getTipoUbicacion().getNombre(),
                asignacion.getEstado().getNombre(),
                asignacion.getObservaciones(),
                asignacion.getFechaCreacion(),
                asignacion.getFechaActualizacion()
        );
    }

    private void publicarAuditoria(
            AsignacionBrigadista asignacion,
            String accion,
            String actorId) {

        eventPublisher.publishEvent(
                new AsignacionAuditEvent(
                        asignacion.getIdAsignacion(),
                        accion,
                        actorId,
                        asignacion.getBrigadistaId(),
                        asignacion.getEventoId(),
                        LocalDateTime.now()
                )
        );
    }
}
