
package edu.eci.dosw.brigades_and_incidents.repository;

import edu.eci.dosw.brigades_and_incidents.entity.Brigadista;
import edu.eci.dosw.brigades_and_incidents.enums.EstadoOperativo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BrigadistaRepository extends JpaRepository<Brigadista, UUID> {

    List<Brigadista> findByEstadoOperativo(EstadoOperativo estadoOperativo);

}
