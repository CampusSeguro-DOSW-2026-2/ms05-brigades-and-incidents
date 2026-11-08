package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.dto.ActualizarEstadoEventoRequest;
import edu.eci.dosw.brigades_and_incidents.entity.ConfiguracionOperacionEvento;
import edu.eci.dosw.brigades_and_incidents.repository.ConfiguracionOperacionEventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

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

        repository.save(configuracion);
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