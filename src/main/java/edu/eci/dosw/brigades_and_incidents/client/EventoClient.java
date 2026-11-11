
package edu.eci.dosw.brigades_and_incidents.client;

public interface EventoClient {

    EventoExterno consultarEvento(String eventoId);

    record EventoExterno(
            String id,
            String nombre,
            String estado
    ) {
    }
}
