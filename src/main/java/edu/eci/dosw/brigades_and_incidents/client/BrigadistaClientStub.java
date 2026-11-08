
package edu.eci.dosw.brigades_and_incidents.client;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("stub")
public class BrigadistaClientStub implements BrigadistaClient {

    private final List<BrigadistaExterno> brigadistas = List.of(
            new BrigadistaExterno(
                    "BRIG-001",
                    "Paula Novoa",
                    "BRIGADISTA"
            ),
            new BrigadistaExterno(
                    "BRIG-002",
                    "Daniel García",
                    "BRIGADISTA"
            ),
            new BrigadistaExterno(
                    "BRIG-003",
                    "Derly Pachón",
                    "BRIGADISTA"
            ),
            new BrigadistaExterno(
                    "BRIG-004",
                    "Jerónimo Moreno",
                    "BRIGADISTA"
            )
    );

    @Override
    public List<BrigadistaExterno> listarBrigadistas() {
        return brigadistas;
    }
}
