package edu.eci.dosw.brigades_and_incidents.repository;

import edu.eci.dosw.brigades_and_incidents.entity.RegistroAuditoria;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegistroAuditoriaRepository
        extends JpaRepository<RegistroAuditoria, UUID> {

    @EntityGraph(attributePaths = "tipoRegistro")
    List<RegistroAuditoria> findByEventoIdOrderByFechaHoraRegistroAsc(
            String eventoId
    );

    @EntityGraph(attributePaths = "tipoRegistro")
    List<RegistroAuditoria> findByRecursoTipoAndRecursoIdOrderByFechaHoraRegistroAsc(
            String recursoTipo,
            String recursoId
    );

    @EntityGraph(attributePaths = "tipoRegistro")
    List<RegistroAuditoria> findAllByOrderByFechaHoraRegistroAsc();
}