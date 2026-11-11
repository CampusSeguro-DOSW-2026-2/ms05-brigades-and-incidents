
package edu.eci.dosw.brigades_and_incidents.repository;

import edu.eci.dosw.brigades_and_incidents.entity.TipoRegistroAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoRegistroAuditoriaRepository
        extends JpaRepository<TipoRegistroAuditoria, Long> {

    Optional<TipoRegistroAuditoria> findByNombre(String nombre);
}
