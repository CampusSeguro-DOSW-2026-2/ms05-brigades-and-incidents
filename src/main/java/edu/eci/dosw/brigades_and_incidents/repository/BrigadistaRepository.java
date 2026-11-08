package edu.eci.dosw.brigades_and_incidents.repository;

import edu.eci.dosw.brigades_and_incidents.entity.Brigadista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrigadistaRepository
        extends JpaRepository<Brigadista, UUID> {

    Optional<Brigadista> findByUsuarioIdExterno(String usuarioIdExterno);

    List<Brigadista> findByUsuarioIdExternoIn(
            Collection<String> usuariosIds
    );
}