package edu.eci.dosw.brigades_and_incidents.repository;

import edu.eci.dosw.brigades_and_incidents.entity.ConfiguracionOperacionEvento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionOperacionEventoRepository
        extends JpaRepository<ConfiguracionOperacionEvento, String> {
}