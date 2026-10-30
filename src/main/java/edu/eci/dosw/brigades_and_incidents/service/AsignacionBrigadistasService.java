
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.dto.AsignacionPredeterminadaRequest;
import edu.eci.dosw.brigades_and_incidents.dto.AsignacionPredeterminadaResponse;
import edu.eci.dosw.brigades_and_incidents.dto.AsignacionPropuestaDTO;
import edu.eci.dosw.brigades_and_incidents.dto.ConfirmarAsignacionesRequest;
import edu.eci.dosw.brigades_and_incidents.dto.HistorialAsignacionDTO;
import edu.eci.dosw.brigades_and_incidents.entity.AsignacionBrigadistas;
import edu.eci.dosw.brigades_and_incidents.entity.Brigadista;
import edu.eci.dosw.brigades_and_incidents.entity.HistorialAsignacion;
import edu.eci.dosw.brigades_and_incidents.enums.EstadoAsignacion;
import edu.eci.dosw.brigades_and_incidents.enums.EstadoOperativo;
import edu.eci.dosw.brigades_and_incidents.enums.TipoAsignacion;
import edu.eci.dosw.brigades_and_incidents.enums.TipoUbicacion;
import edu.eci.dosw.brigades_and_incidents.exception.ConflictoOperacionException;
import edu.eci.dosw.brigades_and_incidents.exception.RecursoNoEncontradoException;
import edu.eci.dosw.brigades_and_incidents.repository.AsignacionBrigadistasRepository;
import edu.eci.dosw.brigades_and_incidents.repository.BrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.ConfiguracionAsignacionRepository;
import edu.eci.dosw.brigades_and_incidents.repository.HistorialAsignacionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class AsignacionBrigadistasService {

    private final AsignacionBrigadistasRepository asignacionRepository;
    private final BrigadistaRepository brigadistaRepository;
    private final ConfiguracionAsignacionRepository configuracionRepository;
    private final HistorialAsignacionRepository historialRepository;
    private final PropuestaAsignacionTemporalService propuestaTemporalService;

    public AsignacionBrigadistasService(
            AsignacionBrigadistasRepository asignacionRepository,
            BrigadistaRepository brigadistaRepository,
            ConfiguracionAsignacionRepository configuracionRepository,
            HistorialAsignacionRepository historialRepository,
            PropuestaAsignacionTemporalService propuestaTemporalService) {

        this.asignacionRepository = asignacionRepository;
        this.brigadistaRepository = brigadistaRepository;
        this.configuracionRepository = configuracionRepository;
        this.historialRepository = historialRepository;
        this.propuestaTemporalService = propuestaTemporalService;
    }

    // Registrar una asignación individual.
    public AsignacionBrigadistas registrar(
            AsignacionBrigadistas asignacion) {

        if (asignacion == null
                || asignacion.getBrigadista() == null
                || asignacion.getBrigadista().getIdBrigadista() == null) {
            throw new IllegalArgumentException(
                    "La información de la asignación y del brigadista es obligatoria");
        }

        Brigadista brigadista = brigadistaRepository
                .findById(asignacion.getBrigadista().getIdBrigadista())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El brigadista no existe"));

        if (brigadista.getEstadoOperativo() != EstadoOperativo.ACTIVO) {
            throw new ConflictoOperacionException(
                    "El brigadista no se encuentra activo");
        }

        if (asignacionRepository
                .existsByBrigadista_IdBrigadistaAndEstado(
                        brigadista.getIdBrigadista(),
                        EstadoAsignacion.VIGENTE)) {
            throw new ConflictoOperacionException(
                    "El brigadista ya tiene una asignación vigente");
        }

        asignacion.setBrigadista(brigadista);

        return asignacionRepository.save(asignacion);
    }

    // Consultar brigadistas activos que no tengan asignaciones vigentes.
    public List<Brigadista> listarBrigadistasActivos() {

        return brigadistaRepository
                .findByEstadoOperativo(EstadoOperativo.ACTIVO)
                .stream()
                .filter(brigadista ->
                        !asignacionRepository
                                .existsByBrigadista_IdBrigadistaAndEstado(
                                        brigadista.getIdBrigadista(),
                                        EstadoAsignacion.VIGENTE))
                .toList();
    }

    // Generar una propuesta automática de asignaciones.
    public AsignacionPredeterminadaResponse prepararPropuestaConResumen(
            AsignacionPredeterminadaRequest solicitud) {

        if (solicitud == null || solicitud.getEventoId() == null) {
            throw new IllegalArgumentException(
                    "El ID del evento es obligatorio");
        }

        if (solicitud.getUbicaciones() == null
                || solicitud.getUbicaciones().isEmpty()) {
            throw new IllegalArgumentException(
                    "El evento debe tener al menos una ubicación");
        }

        Set<UUID> ubicacionesUnicas = new HashSet<>();

        for (AsignacionPredeterminadaRequest.UbicacionSeleccionada ubicacion
                : solicitud.getUbicaciones()) {

            if (ubicacion == null || ubicacion.getUbicacionId() == null) {
                throw new IllegalArgumentException(
                        "Cada ubicación debe tener un ID válido");
            }

            if (!ubicacionesUnicas.add(ubicacion.getUbicacionId())) {
                throw new IllegalArgumentException(
                        "El evento contiene ubicaciones duplicadas");
            }

            convertirTipoUbicacion(ubicacion.getTipoUbicacion());
        }

        // Consultar brigadistas activos sin asignaciones vigentes.
        List<Brigadista> brigadistasDisponibles =
                listarBrigadistasActivos();

        if (brigadistasDisponibles.isEmpty()) {
            throw new ConflictoOperacionException(
                    "No hay brigadistas activos disponibles");
        }

        List<AsignacionPropuestaDTO> propuesta = new ArrayList<>();

        // Se guarda la cantidad correspondiente a cada ubicación.
        Map<UUID, Integer> cantidadesPorUbicacion = new HashMap<>();

        int totalRequeridos = 0;

        // Consultar la configuración de cada ubicación.
        for (AsignacionPredeterminadaRequest.UbicacionSeleccionada ubicacion
                : solicitud.getUbicaciones()) {

            TipoUbicacion tipo = convertirTipoUbicacion(
                    ubicacion.getTipoUbicacion());

            Integer cantidad = configuracionRepository
                    .findByTipoUbicacion(tipo)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe configuración para el tipo de ubicación: "
                                    + tipo))
                    .getCantidadBrigadistas();

            cantidadesPorUbicacion.put(
                    ubicacion.getUbicacionId(),
                    cantidad);

            totalRequeridos += cantidad;
        }

        int cantidadDisponible = brigadistasDisponibles.size();

        // Se conserva el total originalmente requerido.
        int totalRequeridosOriginal = totalRequeridos;

        boolean distribucionAjustada =
                cantidadDisponible < totalRequeridosOriginal;

        // Si no hay suficientes brigadistas, se distribuyen
        // proporcionalmente entre las ubicaciones del evento.
        if (distribucionAjustada) {

            int cantidadBase = cantidadDisponible
                    / solicitud.getUbicaciones().size();

            int sobrantes = cantidadDisponible
                    % solicitud.getUbicaciones().size();

            for (AsignacionPredeterminadaRequest.UbicacionSeleccionada ubicacion
                    : solicitud.getUbicaciones()) {

                int cantidadAjustada = cantidadBase;

                if (sobrantes > 0) {
                    cantidadAjustada++;
                    sobrantes--;
                }

                cantidadesPorUbicacion.put(
                        ubicacion.getUbicacionId(),
                        cantidadAjustada);
            }
        }

        // Generar la propuesta utilizando la cantidad de cada ubicación.
        int indiceBrigadista = 0;

        for (AsignacionPredeterminadaRequest.UbicacionSeleccionada ubicacion
                : solicitud.getUbicaciones()) {

            TipoUbicacion tipo = convertirTipoUbicacion(
                    ubicacion.getTipoUbicacion());

            int cantidad = cantidadesPorUbicacion.get(
                    ubicacion.getUbicacionId());

            for (int i = 0; i < cantidad; i++) {

                if (indiceBrigadista >= cantidadDisponible) {
                    break;
                }

                Brigadista brigadista =
                        brigadistasDisponibles.get(indiceBrigadista++);

                propuesta.add(new AsignacionPropuestaDTO(
                        brigadista.getIdBrigadista(),
                        solicitud.getEventoId(),
                        ubicacion.getUbicacionId(),
                        tipo,
                        TipoAsignacion.DEFAULT
                ));
            }
        }

        // Identificar los brigadistas que no fueron asignados.
        List<UUID> brigadistasSinAsignar = new ArrayList<>();

        for (int i = indiceBrigadista; i < cantidadDisponible; i++) {
            brigadistasSinAsignar.add(
                    brigadistasDisponibles.get(i).getIdBrigadista());
        }

        // Guardar temporalmente la propuesta para su posterior confirmación.
        propuestaTemporalService.guardar(
                solicitud.getEventoId(),
                propuesta,
                solicitud.getUbicaciones()
        );

        return new AsignacionPredeterminadaResponse(
                propuesta,
                brigadistasSinAsignar,
                cantidadDisponible,
                totalRequeridosOriginal,
                distribucionAjustada
        );
    }

    // Confirmar las asignaciones enviadas por MS06.
    @Transactional
    public List<AsignacionBrigadistas> confirmarAsignaciones(
            UUID eventoId,
            ConfirmarAsignacionesRequest solicitud) {

        if (eventoId == null || solicitud == null
                || solicitud.getAsignaciones() == null
                || solicitud.getAsignaciones().isEmpty()) {
            throw new IllegalArgumentException(
                    "El evento y las asignaciones son obligatorios");
        }

        List<AsignacionPropuestaDTO> propuesta =
                propuestaTemporalService.consultar(eventoId);

        List<AsignacionPredeterminadaRequest.UbicacionSeleccionada> ubicaciones =
                propuestaTemporalService.consultarUbicaciones(eventoId);

        Map<UUID, String> ubicacionesDelEvento = new HashMap<>();

        for (AsignacionPredeterminadaRequest.UbicacionSeleccionada ubicacion
                : ubicaciones) {

            ubicacionesDelEvento.put(
                    ubicacion.getUbicacionId(),
                    ubicacion.getTipoUbicacion());
        }

        Set<UUID> brigadistasUnicos = new HashSet<>();

        List<AsignacionBrigadistas> asignacionesGuardar =
                new ArrayList<>();

        for (ConfirmarAsignacionesRequest.AsignacionConfirmada confirmada
                : solicitud.getAsignaciones()) {

            if (confirmada == null
                    || confirmada.getBrigadistaId() == null
                    || confirmada.getUbicacionId() == null
                    || confirmada.getTipoUbicacion() == null) {
                throw new IllegalArgumentException(
                        "Cada asignación debe tener brigadista, ubicación y tipo");
            }

            if (!brigadistasUnicos.add(confirmada.getBrigadistaId())) {
                throw new IllegalArgumentException(
                        "Un brigadista no puede aparecer más de una vez en el evento");
            }

            String tipoEsperado = ubicacionesDelEvento.get(
                    confirmada.getUbicacionId());

            if (tipoEsperado == null) {
                throw new IllegalArgumentException(
                        "La ubicación no pertenece al evento");
            }

            TipoUbicacion tipo = convertirTipoUbicacion(
                    confirmada.getTipoUbicacion());

            if (!tipo.name().equals(tipoEsperado)) {
                throw new IllegalArgumentException(
                        "El tipo de ubicación no coincide con el evento");
            }

            Brigadista brigadista = brigadistaRepository
                    .findById(confirmada.getBrigadistaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe el brigadista con ID: "
                                    + confirmada.getBrigadistaId()));

            if (brigadista.getEstadoOperativo()
                    != EstadoOperativo.ACTIVO) {
                throw new ConflictoOperacionException(
                        "El brigadista no se encuentra activo");
            }

            if (asignacionRepository
                    .existsByBrigadista_IdBrigadistaAndEstado(
                            brigadista.getIdBrigadista(),
                            EstadoAsignacion.VIGENTE)) {
                throw new ConflictoOperacionException(
                        "El brigadista ya tiene una asignación vigente");
            }

            boolean estabaEnPropuesta = propuesta.stream().anyMatch(
                    asignacion -> asignacion.brigadistaId().equals(
                            confirmada.getBrigadistaId())
                            && asignacion.ubicacionId().equals(
                                    confirmada.getUbicacionId()));

            TipoAsignacion tipoAsignacion = estabaEnPropuesta
                    ? TipoAsignacion.DEFAULT
                    : TipoAsignacion.MANUAL;

            AsignacionBrigadistas asignacion =
                    new AsignacionBrigadistas();

            asignacion.setBrigadista(brigadista);
            asignacion.setEventoId(eventoId);
            asignacion.setUbicacionId(confirmada.getUbicacionId());
            asignacion.setTipoUbicacion(tipo);
            asignacion.setTipoAsignacion(tipoAsignacion);
            asignacion.setEstado(EstadoAsignacion.VIGENTE);
            asignacion.setObservaciones(confirmada.getObservaciones());

            asignacionesGuardar.add(asignacion);
        }

        List<AsignacionBrigadistas> guardadas =
                asignacionRepository.saveAll(asignacionesGuardar);

        propuestaTemporalService.eliminar(eventoId);

        return guardadas;
    }

    // Consultar asignaciones por evento.
    public List<AsignacionBrigadistas> listarPorEvento(UUID eventoId) {
        return asignacionRepository.findByEventoId(eventoId);
    }

    // Consultar asignaciones por brigadista.
    public List<AsignacionBrigadistas> listarPorBrigadista(
            UUID brigadistaId) {
        return asignacionRepository.findByBrigadista_IdBrigadista(
                brigadistaId);
    }

    // Consultar asignaciones por estado.
    public List<AsignacionBrigadistas> listarPorEstado(
            EstadoAsignacion estado) {
        return asignacionRepository.findByEstado(estado);
    }

    // Retirar una asignación.
    @Transactional
    public AsignacionBrigadistas retirar(UUID asignacionId) {

        AsignacionBrigadistas asignacion = asignacionRepository
                .findById(asignacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la asignación con ID: " + asignacionId));

        asignacion.setEstado(EstadoAsignacion.RETIRADA);

        return asignacionRepository.save(asignacion);
    }

    // Modificar la ubicación de una asignación.
    @Transactional
    public AsignacionBrigadistas modificar(
            UUID asignacionId,
            UUID nuevaUbicacionId,
            TipoUbicacion nuevoTipoUbicacion,
            String observaciones) {

        AsignacionBrigadistas asignacion = asignacionRepository
                .findById(asignacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la asignación con ID: " + asignacionId));

        if (asignacion.getEstado() != EstadoAsignacion.VIGENTE) {
            throw new ConflictoOperacionException(
                    "Solo se pueden modificar asignaciones vigentes");
        }

        if (nuevaUbicacionId == null || nuevoTipoUbicacion == null) {
            throw new IllegalArgumentException(
                    "La nueva ubicación y su tipo son obligatorios");
        }

        boolean cambioUbicacion = !Objects.equals(
                asignacion.getUbicacionId(),
                nuevaUbicacionId);

        if (cambioUbicacion) {

            HistorialAsignacion historial = new HistorialAsignacion();

            historial.setAsignacion(asignacion);
            historial.setUbicacionAnteriorId(
                    asignacion.getUbicacionId());
            historial.setTipoUbicacionAnterior(
                    asignacion.getTipoUbicacion());
            historial.setUbicacionNuevaId(nuevaUbicacionId);
            historial.setTipoUbicacionNueva(nuevoTipoUbicacion);

            historialRepository.save(historial);
        }

        asignacion.setUbicacionId(nuevaUbicacionId);
        asignacion.setTipoUbicacion(nuevoTipoUbicacion);
        asignacion.setTipoAsignacion(TipoAsignacion.MANUAL);
        asignacion.setObservaciones(observaciones);

        return asignacionRepository.save(asignacion);
    }

    // Consultar el historial de cambios de una asignación.
    public List<HistorialAsignacionDTO> consultarHistorial(
            UUID asignacionId) {

        if (asignacionId == null) {
            throw new IllegalArgumentException(
                    "El ID de la asignación es obligatorio");
        }

        if (!asignacionRepository.existsById(asignacionId)) {
            throw new RecursoNoEncontradoException(
                    "No existe la asignación con ID: " + asignacionId);
        }

        List<HistorialAsignacion> historial =
                historialRepository
                        .findByAsignacion_IdAsignacionOrderByFechaCambioAsc(
                                asignacionId);

        return historial.stream()
                .map(registro -> new HistorialAsignacionDTO(
                        registro.getIdHistorial(),
                        registro.getAsignacion().getIdAsignacion(),
                        registro.getUbicacionAnteriorId(),
                        registro.getTipoUbicacionAnterior(),
                        registro.getUbicacionNuevaId(),
                        registro.getTipoUbicacionNueva(),
                        registro.getFechaCambio()
                ))
                .toList();
    }

    // Convertir y validar el tipo de ubicación.
    private TipoUbicacion convertirTipoUbicacion(String tipo) {

        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException(
                    "El tipo de ubicación es obligatorio");
        }

        try {
            return TipoUbicacion.valueOf(
                    tipo.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Tipo de ubicación no válido: " + tipo);
        }
    }
}
