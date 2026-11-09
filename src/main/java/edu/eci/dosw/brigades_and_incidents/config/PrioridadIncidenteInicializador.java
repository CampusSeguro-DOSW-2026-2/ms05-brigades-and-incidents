package edu.eci.dosw.brigades_and_incidents.config;

import edu.eci.dosw.brigades_and_incidents.entity.PrioridadIncidente;
import edu.eci.dosw.brigades_and_incidents.repository.PrioridadIncidenteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PrioridadIncidenteInicializador implements CommandLineRunner {

    private final PrioridadIncidenteRepository prioridadRepository;

    public PrioridadIncidenteInicializador(
            PrioridadIncidenteRepository prioridadRepository) {
        this.prioridadRepository = prioridadRepository;
    }

    @Override
    public void run(String... args) {

        List<PrioridadIncidente> prioridades = List.of(
                new PrioridadIncidente(
                        "EMERGENCIA",
                        "Prioridad alta. Situaciones con riesgo vital."
                ),
                new PrioridadIncidente(
                        "URGENCIA_MODERADA",
                        "Prioridad media. Situaciones que requieren atención."
                ),
                new PrioridadIncidente(
                        "POCA_URGENCIA",
                        "Prioridad baja. Situaciones sin riesgo vital inmediato."
                )
        );

        for (PrioridadIncidente prioridad : prioridades) {
            if (prioridadRepository.findByNombre(prioridad.getNombre()).isEmpty()) {
                prioridadRepository.save(prioridad);
            }
        }
    }
}