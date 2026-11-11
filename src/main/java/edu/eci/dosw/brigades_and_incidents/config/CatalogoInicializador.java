
package edu.eci.dosw.brigades_and_incidents.config;

import edu.eci.dosw.brigades_and_incidents.entity.EstadoAsignacion;
import edu.eci.dosw.brigades_and_incidents.entity.TipoUbicacion;
import edu.eci.dosw.brigades_and_incidents.repository.EstadoAsignacionRepository;
import edu.eci.dosw.brigades_and_incidents.repository.TipoUbicacionRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogoInicializador {

    @Bean
    CommandLineRunner inicializarCatalogos(
            EstadoAsignacionRepository estadoRepository,
            TipoUbicacionRepository tipoRepository) {

        return args -> {

            if (estadoRepository.findByNombre("VIGENTE").isEmpty()) {
                estadoRepository.save(
                        new EstadoAsignacion(
                                "VIGENTE",
                                "La asignación se encuentra activa"
                        )
                );
            }

            if (estadoRepository.findByNombre("RETIRADA").isEmpty()) {
                estadoRepository.save(
                        new EstadoAsignacion(
                                "RETIRADA",
                                "La asignación fue retirada"
                        )
                );
            }

            crearTipoSiNoExiste(
                    tipoRepository,
                    "EDIFICIO",
                    "Asignación correspondiente a un edificio"
            );

            crearTipoSiNoExiste(
                    tipoRepository,
                    "ZONA_SEGURA",
                    "Asignación correspondiente a una zona segura"
            );

            crearTipoSiNoExiste(
                    tipoRepository,
                    "PUNTO_ENCUENTRO",
                    "Asignación correspondiente a un punto de encuentro"
            );
        };
    }

    private void crearTipoSiNoExiste(
            TipoUbicacionRepository repository,
            String nombre,
            String descripcion) {

        if (repository.findByNombre(nombre).isEmpty()) {
            repository.save(new TipoUbicacion(nombre, descripcion));
        }
    }
}
