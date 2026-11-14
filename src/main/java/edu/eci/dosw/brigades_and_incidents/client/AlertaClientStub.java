package edu.eci.dosw.brigades_and_incidents.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Perfil stub: no llama a MS03, solo deja constancia del aviso en el log.
 */
@Component
@Profile("stub")
public class AlertaClientStub implements AlertaClient {

    private static final Logger LOG = LoggerFactory.getLogger(AlertaClientStub.class);

    @Override
    public void notificarAsignacion(NotificacionAsignacion notificacion) {
        LOG.info("Aviso de asignación (stub): brigadista {} en la solicitud {}",
                notificacion.brigadistaId(), notificacion.incidenteId());
    }
}
