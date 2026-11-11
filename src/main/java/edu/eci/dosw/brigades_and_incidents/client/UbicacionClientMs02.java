
package edu.eci.dosw.brigades_and_incidents.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
@Profile("!stub & !local")
public class UbicacionClientMs02 implements UbicacionClient {

    private final RestClient restClient;

    public UbicacionClientMs02(
            RestClient.Builder builder,
            @Value("${campusseguro.ms02.base-url}") String baseUrl
    ) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public UbicacionExterna consultarUbicacion(
            String ubicacionId,
            String tipoUbicacion
    ) {
        if (ubicacionId == null || ubicacionId.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador de la ubicación es obligatorio."
            );
        }

        if (tipoUbicacion == null || tipoUbicacion.isBlank()) {
            throw new IllegalArgumentException(
                    "El tipo de ubicación es obligatorio."
            );
        }

        try {
            String tipo = tipoUbicacion.trim().toUpperCase();

            return switch (tipo) {
                case "EDIFICIO" -> consultarEdificio(ubicacionId);
                case "ZONA_SEGURA" -> consultarZonaSegura(
                        ubicacionId, false
                );
                case "PUNTO_ENCUENTRO" -> consultarZonaSegura(
                        ubicacionId, true
                );
                default -> throw new IllegalArgumentException(
                        "Tipo de ubicación no soportado: " + tipo
                );
            };

        } catch (HttpClientErrorException.NotFound exception) {
            return null;

        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No fue posible consultar la ubicación en MS02.",
                    exception
            );
        }
    }

    private UbicacionExterna consultarEdificio(String id) {

        EdificioResponse response = restClient.get()
                .uri("/api/v1/buildings/{id}", id)
                .retrieve()
                .body(EdificioResponse.class);

        if (response == null) {
            return null;
        }

        return new UbicacionExterna(
                response.id().toString(),
                response.nombre(),
                "EDIFICIO",
                "ACTIVO".equalsIgnoreCase(response.estado())
        );
    }

    private UbicacionExterna consultarZonaSegura(
            String id,
            boolean puntoEncuentro
    ) {
        ZonaSeguraResponse response = restClient.get()
                .uri("/api/v1/safe-zones/{id}", id)
                .retrieve()
                .body(ZonaSeguraResponse.class);

        if (response == null) {
            return null;
        }

        if (response.puntoEncuentro() != puntoEncuentro) {
            return null;
        }

        return new UbicacionExterna(
                response.id().toString(),
                response.nombre(),
                puntoEncuentro ? "PUNTO_ENCUENTRO" : "ZONA_SEGURA",
                "ACTIVO".equalsIgnoreCase(response.estado())
        );
    }

    private record EdificioResponse(
            UUID id,
            String nombre,
            String estado
    ) {}

    private record ZonaSeguraResponse(
            UUID id,
            String nombre,
            String estado,
            boolean puntoEncuentro
    ) {}
}
