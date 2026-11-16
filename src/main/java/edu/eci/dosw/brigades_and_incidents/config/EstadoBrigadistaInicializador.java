package edu.eci.dosw.brigades_and_incidents.config;

import edu.eci.dosw.brigades_and_incidents.entity.EstadoBrigadista;
import edu.eci.dosw.brigades_and_incidents.repository.EstadoBrigadistaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class EstadoBrigadistaInicializador implements CommandLineRunner {

    private final EstadoBrigadistaRepository estadoRepository;

    public EstadoBrigadistaInicializador(
            EstadoBrigadistaRepository estadoRepository
    ) {
        this.estadoRepository = estadoRepository;
    }

    @Override
    public void run(String... args) {

        crearEstadoSiNoExiste(
                "INACTIVO",
                "Brigadista registrado que aún no está habilitado"
        );

        crearEstadoSiNoExiste(
                "ACTIVO",
                "Brigadista habilitado para participar"
        );
    }

    private void crearEstadoSiNoExiste(
            String nombre,
            String descripcion
    ) {
        if (estadoRepository.findByNombre(nombre).isEmpty()) {
            estadoRepository.save(
                    new EstadoBrigadista(nombre, descripcion)
            );
        }
    }
}