
package edu.eci.dosw.brigades_and_incidents.client;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Component
@Profile("!stub & !local")
public class UbicacionClientMs02 implements UbicacionClient {

    private static final List<String> CABECERAS_PROPAGADAS =
            List.of("X-User-Id", "X-User-Role", "X-Correlation-ID");

    private final RestClient restClient;

    public UbicacionClientMs02(
            RestClient.Builder builder,
            @Value("${campusseguro.ms02.base-url}") String baseUrl,
            @Value("${campusseguro.ms02.service-key:}") String serviceKey
    ) {
        // MS02 se consulta a través del Gateway con la clave de MS05 (Estándares §5).
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("X-Service-Key", serviceKey)
                .build();
    }

    /**
     * MS02 autoriza por la identidad del usuario: se propagan sin cambios X-User-Id, X-User-Role y
     * X-Correlation-ID de la solicitud en curso (Estándares §5).
     */
    private static void propagarIdentidad(HttpHeaders headers) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            HttpServletRequest solicitud = atributos.getRequest();
            for (String nombre : CABECERAS_PROPAGADAS) {
                String valor = solicitud.getHeader(nombre);
                if (valor != null && !valor.isBlank()) {
                    headers.set(nombre, valor);
                }
            }
        }
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
                .headers(UbicacionClientMs02::propagarIdentidad)
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
                .headers(UbicacionClientMs02::propagarIdentidad)
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
