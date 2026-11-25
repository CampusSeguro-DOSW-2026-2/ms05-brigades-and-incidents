package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.dto.ActualizarEstadoEventoRequest;
import edu.eci.dosw.brigades_and_incidents.entity.ConfiguracionOperacionEvento;
import edu.eci.dosw.brigades_and_incidents.entity.ZonaEvento;
import edu.eci.dosw.brigades_and_incidents.repository.ConfiguracionOperacionEventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class GestionOperacionEventoService {

    private final ConfiguracionOperacionEventoRepository repository;

    public GestionOperacionEventoService(
            ConfiguracionOperacionEventoRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public void actualizarOperacion(
            String eventoId,
            ActualizarEstadoEventoRequest request
    ) {

        if (request == null || request.eventoId() == null
                || !eventoId.equals(request.eventoId())) {
            throw new IllegalArgumentException(
                    "El identificador del evento no coincide con la solicitud"
            );
        }

        if (request.estado() == null || request.estado().isBlank()) {
            throw new IllegalArgumentException(
                    "El estado del evento es obligatorio"
            );
        }

        String estado = request.estado()
                .trim()
                .toUpperCase(Locale.ROOT);

        ConfiguracionOperacionEvento configuracion =
                repository.findById(eventoId)
                        .orElseGet(() ->
                                new ConfiguracionOperacionEvento(eventoId)
                        );

        switch (estado) {
            case "PROGRAMADO" ->
                    configuracion.habilitarAutoasignacion();

            case "EN_EJECUCION" ->
                    configuracion.habilitarPanel();

            case "FINALIZADO", "BORRADOR" ->
                    configuracion.deshabilitarOperacion();

            default -> throw new IllegalArgumentException(
                    "Estado de evento no soportado: " + estado
            );
        }

        configuracion.registrarDatosEvento(
                request.nombre(),
                request.simulacro(),
                estado,
                convertirZonas(request.zonas(), request.edificios())
        );

        repository.save(configuracion);
    }

    /**
     * Alcance del evento: zonas y edificios que envía MS06. MS06 manda los edificios aparte, solo con su id; se suman
     * como ubicaciones tipo EDIFICIO. Se ignoran los que llegan sin identificador y, si un id viene en las dos listas,
     * se conserva el de {@code zonas}, que trae nombre y tipo.
     */
    private List<ZonaEvento> convertirZonas(
            List<ActualizarEstadoEventoRequest.ZonaEventoDTO> zonas,
            List<String> edificios
    ) {
        List<ZonaEvento> alcance = new ArrayList<>();
        if (zonas != null) {
            zonas.stream()
                    .filter(Objects::nonNull)
                    .filter(zona -> zona.zonaId() != null && !zona.zonaId().isBlank())
                    .map(zona -> new ZonaEvento(
                            zona.zonaId().trim(),
                            zona.nombre(),
                            normalizarTipo(zona.tipo())
                    ))
                    .forEach(alcance::add);
        }
        if (edificios != null) {
            edificios.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(id -> !id.isEmpty())
                    .map(id -> new ZonaEvento(id, null, ZonaEvento.TIPO_POR_DEFECTO))
                    .forEach(alcance::add);
        }
        return alcance.stream().distinct().toList();
    }

    /**
     * MS06 nombra el alcance BUILDING/ZONE; MS05 usa EDIFICIO/ZONA_SEGURA/PUNTO_ENCUENTRO. MS06 manda en
     * {@code zonas} solo su alcance ZONE y sin tipo, por eso el valor por defecto de esa lista es ZONA_SEGURA.
     */
    private String normalizarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "ZONA_SEGURA";
        }
        String valor = tipo.trim().toUpperCase(Locale.ROOT);
        return switch (valor) {
            case "ZONE", "ZONA", "ZONA_SEGURA" -> "ZONA_SEGURA";
            case "PUNTO_ENCUENTRO", "MEETING_POINT" -> "PUNTO_ENCUENTRO";
            default -> ZonaEvento.TIPO_POR_DEFECTO;
        };
    }

    @Transactional(readOnly = true)
    public ConfiguracionOperacionEvento obtenerConfiguracion(
            String eventoId
    ) {
        return repository.findById(eventoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe configuración operativa para el evento"
                ));
    }
}