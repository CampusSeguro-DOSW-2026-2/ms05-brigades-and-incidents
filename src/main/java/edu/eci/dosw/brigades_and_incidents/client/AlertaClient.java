package edu.eci.dosw.brigades_and_incidents.client;

import java.util.Set;

/**
 * Avisos a los usuarios a través de MS03 (Alertas), que es quien envía los correos.
 */
public interface AlertaClient {

    /**
     * Avisa al brigadista que se le asignó una solicitud de atención.
     *
     * @throws RuntimeException si MS03 no recibe el aviso; quien llama decide si eso es grave
     */
    void notificarAsignacion(NotificacionAsignacion notificacion);

    /**
     * Datos del aviso de asignación.
     *
     * @param brigadistaId  usuario (MS01) que recibe el aviso
     * @param actorId       usuario que asignó (coordinador u organizador)
     * @param actorRoles    roles del usuario que asignó
     * @param eventoId      evento de MS06 al que pertenece la solicitud
     * @param incidenteId   solicitud de atención asignada
     * @param prioridad     nombre de la prioridad de la solicitud
     * @param ubicacionId   ubicación de la solicitud (id de MS02)
     * @param tipoUbicacion EDIFICIO, ZONA_SEGURA o PUNTO_ENCUENTRO
     */
    record NotificacionAsignacion(
            String brigadistaId,
            String actorId,
            Set<String> actorRoles,
            String eventoId,
            String incidenteId,
            String prioridad,
            String ubicacionId,
            String tipoUbicacion
    ) {

        public NotificacionAsignacion {
            actorRoles = actorRoles == null ? Set.of() : Set.copyOf(actorRoles);
        }
    }
}
