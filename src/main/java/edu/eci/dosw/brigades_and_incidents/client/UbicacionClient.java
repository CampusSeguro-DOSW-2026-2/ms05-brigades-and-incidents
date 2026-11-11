
package edu.eci.dosw.brigades_and_incidents.client;

public interface UbicacionClient {

    UbicacionExterna consultarUbicacion(
            String ubicacionId,
            String tipoUbicacion
    );

    record UbicacionExterna(
            String id,
            String nombre,
            String tipo,
            boolean activa
    ) {
    }
}
