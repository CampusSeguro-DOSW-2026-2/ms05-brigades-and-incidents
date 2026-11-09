
package edu.eci.dosw.brigades_and_incidents.repository;

import edu.eci.dosw.brigades_and_incidents.entity.Incidente;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class IncidenteSpecification {

    private IncidenteSpecification() {
    }

    public static Specification<Incidente> porEvento(String eventoId) {
        return (root, query, cb) ->
                cb.equal(root.get("eventoId"), eventoId);
    }

    /**
     * Excluye los incidentes que fueron eliminados logicamente.
     */
    public static Specification<Incidente> noEliminados() {
        return (root, query, cb) ->
                cb.isFalse(root.get("eliminado"));
    }

    public static Specification<Incidente> porPrioridad(String prioridad) {
        return (root, query, cb) -> {
            if (prioridad == null || prioridad.isBlank()) {
                return cb.conjunction();
            }

            return cb.equal(
                    cb.lower(root.join("prioridad").get("nombre")),
                    prioridad.trim().toLowerCase()
            );
        };
    }

    public static Specification<Incidente> porEstado(String estado) {
        return (root, query, cb) -> {
            if (estado == null || estado.isBlank()) {
                return cb.conjunction();
            }

            return cb.equal(
                    cb.lower(root.join("estado").get("nombre")),
                    estado.trim().toLowerCase()
            );
        };
    }

    public static Specification<Incidente> porUbicacion(String ubicacionId) {
        return (root, query, cb) -> {
            if (ubicacionId == null || ubicacionId.isBlank()) {
                return cb.conjunction();
            }

            return cb.equal(
                    root.get("ubicacionId"),
                    ubicacionId.trim()
            );
        };
    }

    public static Specification<Incidente> visiblesParaBrigadista(
            String brigadistaId,
            List<String> ubicacionesAsignadas
    ) {
        return (root, query, cb) -> {

            var reportadosPorElBrigadista =
                    cb.equal(root.get("reportanteId"), brigadistaId);

            var asignadosAlBrigadista =
                    cb.equal(
                            root.get("brigadistaResponsableId"),
                            brigadistaId
                    );

            if (ubicacionesAsignadas == null
                    || ubicacionesAsignadas.isEmpty()) {
                return cb.or(
                        reportadosPorElBrigadista,
                        asignadosAlBrigadista
                );
            }

            var ubicacionAsignada =
                    root.get("ubicacionId").in(ubicacionesAsignadas);

            return cb.or(
                    reportadosPorElBrigadista,
                    asignadosAlBrigadista,
                    ubicacionAsignada
            );
        };
    }
}
