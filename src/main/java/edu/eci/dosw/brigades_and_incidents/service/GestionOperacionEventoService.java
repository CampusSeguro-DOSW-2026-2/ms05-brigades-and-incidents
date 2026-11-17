package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.dto.ActualizarEstadoEventoRequest;
import edu.eci.dosw.brigades_and_incidents.entity.ConfiguracionOperacionEvento;
import edu.eci.dosw.brigades_and_incidents.entity.ZonaEvento;
import edu.eci.dosw.brigades_and_incidents.repository.ConfiguracionOperacionEventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                convertirZonas(request.zonas())
        );

        repository.save(configuracion);
    }

    /** Edificios del alcance que envía MS06; se ignoran los que llegan sin identificador. */
    private List<ZonaEvento> convertirZonas(
            List<ActualizarEstadoEventoRequest.ZonaEventoDTO> zonas
    ) {
        if (zonas == null) {
            return List.of();
        }
        return zonas.stream()
                .filter(Objects::nonNull)
                .filter(zona -> zona.zonaId() != null && !zona.zonaId().isBlank())
                .map(zona -> new ZonaEvento(
                        zona.zonaId().trim(),
                        zona.nombre(),
                        normalizarTipo(zona.tipo())
                ))
                .distinct()
                .toList();
    }

    /** MS06 nombra el alcance BUILDING/ZONE; MS05 usa EDIFICIO/ZONA_SEGURA/PUNTO_ENCUENTRO. */
    private String normalizarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return ZonaEvento.TIPO_POR_DEFECTO;
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