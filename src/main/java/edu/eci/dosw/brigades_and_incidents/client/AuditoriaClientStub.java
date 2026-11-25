package edu.eci.dosw.brigades_and_incidents.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Perfil stub: no llama a MS08, solo deja constancia del hecho de auditoría en el log.
 */
@Component
@Profile("stub")
public class AuditoriaClientStub implements AuditoriaClient {

    private static final Logger LOG = LoggerFactory.getLogger(AuditoriaClientStub.class);

    @Override
    public void registrarEvento(EventoAuditoria evento) {
        if (evento == null) {
            return;
        }
        LOG.info("Auditoría MS08 (stub): accion={} recurso={}:{} resultado={}",
                evento.accion(), evento.recursoTipo(), evento.recursoId(), evento.resultado());
    }
}