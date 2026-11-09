package edu.eci.dosw.brigades_and_incidents.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.eci.dosw.brigades_and_incidents.exception.Ms01IntegrationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Profile("!stub")
public class UsuarioClientMs01 implements UsuarioClient {

    private final RestClient restClient;

    public UsuarioClientMs01(
            @Value("${campusseguro.ms01.base-url}") String baseUrl,
            @Value("${campusseguro.ms01.service-key}") String serviceKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Service-Key", serviceKey)
                .build();
    }

    @Override
    public UsuarioExterno consultarUsuario(String usuarioId) {

        UUID id;

        try {
            id = UUID.fromString(usuarioId);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new Ms01IntegrationException(
                    "El identificador del usuario no es un UUID válido",
                    exception
            );
        }

        Ms01BatchResponse response;

        try {
            response = restClient.post()
                    .uri("/api/v1/users/batch")
                    .body(new Ms01BatchRequest(List.of(id)))
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
                    .body(Ms01BatchResponse.class);

        } catch (Ms01IntegrationException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new Ms01IntegrationException(
                    "No fue posible consultar el usuario en MS01",
                    exception
            );
        }

        if (response == null || response.usuarios() == null) {
            throw new Ms01IntegrationException(
                    "MS01 devolvió una respuesta vacía o inválida"
            );
        }

        return response.usuarios().stream()
                .filter(Objects::nonNull)
                .filter(usuario -> id.equals(usuario.id()))
                .findFirst()
                .map(this::convertirUsuario)
                .orElse(null);
    }

    private UsuarioExterno convertirUsuario(Ms01UserResponse usuario) {

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

        return new UsuarioExterno(
                usuario.id().toString(),
                nombreCompleto,
                usuario.roles() == null ? List.of() : usuario.roles(),
                usuario.estado()
        );
    }

    private record Ms01BatchRequest(
            @JsonProperty("ids")
            List<UUID> ids
    ) {
    }

    private record Ms01BatchResponse(
            @JsonProperty("usuarios")
            List<Ms01UserResponse> usuarios,

            @JsonProperty("no_encontrados")
            List<UUID> noEncontrados
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
            List<String> roles,

            @JsonProperty("estado")
            String estado
    ) {
    }
}
