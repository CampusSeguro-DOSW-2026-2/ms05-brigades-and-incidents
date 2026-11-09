
package edu.eci.dosw.brigades_and_incidents.client;

import java.util.List;

public interface BrigadistaClient {

    List<BrigadistaExterno> listarBrigadistas();

    record BrigadistaExterno(
            String id,
            String nombre,
            String rol
    ) {
    }
}
