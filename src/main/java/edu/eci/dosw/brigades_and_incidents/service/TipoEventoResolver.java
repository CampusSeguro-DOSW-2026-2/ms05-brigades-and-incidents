package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.entity.ConfiguracionOperacionEvento;
import edu.eci.dosw.brigades_and_incidents.repository.ConfiguracionOperacionEventoRepository;
import org.springframework.stereotype.Component;

/**
 * Indica si un evento es simulacro o real con la configuración que MS06 registra en MS05. Sirve para exigir el
 * permiso de simulacros (assign_to_drill) o el de eventos reales (assign_to_real) según el evento.
 */
@Component
public class TipoEventoResolver {

    private final ConfiguracionOperacionEventoRepository repository;

    public TipoEventoResolver(ConfiguracionOperacionEventoRepository repository) {
        this.repository = repository;
    }

    /** true si es simulacro, false si es real y null si el evento aún no está registrado en MS05. */
    public Boolean esSimulacro(String eventoId) {
        if (eventoId == null || eventoId.isBlank()) {
            return null;
        }
        return repository.findById(eventoId)
                .map(ConfiguracionOperacionEvento::isSimulacro)
                .orElse(null);
    }
}
