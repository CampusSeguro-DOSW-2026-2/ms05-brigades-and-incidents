
package edu.eci.dosw.brigades_and_incidents.client;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile({"stub", "local"})
public class UbicacionClientStub implements UbicacionClient {

    private final List<UbicacionExterna> ubicaciones = List.of(
            new UbicacionExterna(
                    "EDIF-001",
                    "Edificio A",
                    "EDIFICIO",
                    true
            ),
            new UbicacionExterna(
                    "ZONA-001",
                    "Zona segura norte",
                    "ZONA_SEGURA",
                    true
            ),
            new UbicacionExterna(
                    "PUNTO-001",
                    "Punto de encuentro principal",
                    "PUNTO_ENCUENTRO",
                    true
            ),
            new UbicacionExterna(
                    "EDIF-002",
                    "Edificio fuera de servicio",
                    "EDIFICIO",
                    false
            )
    );

    @Override
    public UbicacionExterna consultarUbicacion(
            String ubicacionId,
            String tipoUbicacion
    ) {
        return ubicaciones.stream()
                .filter(ubicacion -> ubicacion.id().equals(ubicacionId))
                .filter(ubicacion -> ubicacion.tipo().equals(tipoUbicacion))
                .findFirst()
                .orElse(null);
    }
}
