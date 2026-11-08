
package edu.eci.dosw.brigades_and_incidents.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.eci.dosw.brigades_and_incidents.exception.Ms01IntegrationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Profile("!stub")
public class BrigadistaClientMs01 implements BrigadistaClient {

    private static final int PAGE_SIZE = 100;

    private final RestClient restClient;

    public BrigadistaClientMs01(
            @Value("${campusseguro.ms01.base-url}") String baseUrl,
            @Value("${campusseguro.ms01.service-key}") String serviceKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Service-Key", serviceKey)
                .build();
    }

    @Override
    public List<BrigadistaExterno> listarBrigadistas() {

        List<BrigadistaExterno> brigadistas = new ArrayList<>();
        int page = 0;
        int totalPages;

        do {
            int currentPage = page;

            Ms01PageResponse response;

            try {
                response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/v1/users/by-role")
                                .queryParam("rol", "BRIGADISTA")
                                .queryParam("page", currentPage)
                                .queryParam("size", PAGE_SIZE)
                                .build())
                        .retrieve()
                        .onStatus(
                                status -> status.isError(),
                                (request, responseHttp) -> {
                                    throw new Ms01IntegrationException(
                                            "MS01 respondió con estado HTTP "
                                                    + responseHttp.getStatusCode()
                                    );
                                }
                        )
                        .body(Ms01PageResponse.class);

            } catch (Ms01IntegrationException exception) {
                throw exception;
            } catch (RestClientException exception) {
                throw new Ms01IntegrationException(
                        "No fue posible comunicarse con MS01 a través del Gateway",
                        exception
                );
            }

            if (response == null) {
                throw new Ms01IntegrationException(
                        "MS01 devolvió una respuesta vacía"
                );
            }

            if (response.totalPaginas() < 0) {
                throw new Ms01IntegrationException(
                        "MS01 devolvió un número de páginas inválido"
                );
            }

            if (response.contenido() != null) {
                response.contenido().stream()
                        .filter(Objects::nonNull)
                        .map(this::convertirBrigadista)
                        .forEach(brigadistas::add);
            }

            totalPages = response.totalPaginas();
            page++;

        } while (page < totalPages);

        return brigadistas;
    }

    private BrigadistaExterno convertirBrigadista(
            Ms01UserResponse usuario
    ) {
        UUID id = Objects.requireNonNull(
                usuario.id(),
                "MS01 devolvió un usuario sin identificador"
        );

        String nombreCompleto = Stream.of(
                        usuario.primerNombre(),
                        usuario.segundoNombre(),
                        usuario.primerApellido(),
                        usuario.segundoApellido()
                )
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(nombre -> !nombre.isBlank())
                .collect(Collectors.joining(" "));

        String rol = usuario.roles() == null
                ? null
                : usuario.roles().stream()
                        .filter(Objects::nonNull)
                        .filter("BRIGADISTA"::equalsIgnoreCase)
                        .findFirst()
                        .orElse(null);

        return new BrigadistaExterno(
                id.toString(),
                nombreCompleto,
                rol
        );
    }

    private record Ms01PageResponse(
            @JsonProperty("contenido")
            List<Ms01UserResponse> contenido,

            @JsonProperty("page")
            int page,

            @JsonProperty("size")
            int size,

            @JsonProperty("total_elementos")
            long totalElementos,

            @JsonProperty("total_paginas")
            int totalPaginas
    ) {
    }

    private record Ms01UserResponse(
            @JsonProperty("id")
            UUID id,

            @JsonProperty("primer_nombre")
            String primerNombre,

            @JsonProperty("segundo_nombre")
            String segundoNombre,

            @JsonProperty("primer_apellido")
            String primerApellido,

            @JsonProperty("segundo_apellido")
            String segundoApellido,

            @JsonProperty("roles")
            List<String> roles
    ) {
    }
}
