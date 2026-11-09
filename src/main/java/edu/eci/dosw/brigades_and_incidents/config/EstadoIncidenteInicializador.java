package edu.eci.dosw.brigades_and_incidents.config;

import edu.eci.dosw.brigades_and_incidents.entity.EstadoIncidente;
import edu.eci.dosw.brigades_and_incidents.repository.EstadoIncidenteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EstadoIncidenteInicializador implements CommandLineRunner {

    private final EstadoIncidenteRepository estadoRepository;

    public EstadoIncidenteInicializador(
            EstadoIncidenteRepository estadoRepository) {
        this.estadoRepository = estadoRepository;
    }

    @Override
    public void run(String... args) {

        List<EstadoIncidente> estados = List.of(
                new EstadoIncidente(
                        "REPORTADO",
                        "Incidente registrado sin brigadistas asignados a la ubicación."
                ),
                new EstadoIncidente(
                        "ASIGNADO",
                        "Incidente registrado con brigadistas disponibles para atenderlo."
                )
        );

        for (EstadoIncidente estado : estados) {
            if (estadoRepository.findByNombre(estado.getNombre()).isEmpty()) {
                estadoRepository.save(estado);
            }
        }
    }
}