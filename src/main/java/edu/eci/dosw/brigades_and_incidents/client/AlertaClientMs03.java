package edu.eci.dosw.brigades_and_incidents.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Crea en MS03, a través del API Gateway (Estándares §5), una alerta dirigida solo al brigadista
 * asignado. MS03 la emite de inmediato y le envía el correo.
 *
 * <ul>
 *   <li>Viaja con la clave de servicio de MS05 y la identidad de quien asignó
 *       ({@code X-User-Id}, {@code X-User-Role}), porque MS03 autoriza según ese usuario.</li>
 *   <li>El coordinador crea una alerta MANUAL de tipo OPERATIVA.</li>
 *   <li>El organizador solo puede crear alertas de simulacro: la alerta va asociada al evento
 *       (SIMULATION) con un {@code purpose} propio de la solicitud y del brigadista, para no chocar
 *       con las demás alertas vivas del evento.</li>
 * </ul>
 */
@Component
@Profile("!stub")
public class AlertaClientMs03 implements AlertaClient {

    private static final String RUTA_ALERTAS = "/api/v1/alertas";
    private static final String TITULO = "Solicitud de atención asignada";
    private static final int MAX_PURPOSE = 32;

    private final RestClient restClient;

    public AlertaClientMs03(
            @Value("${campusseguro.ms03.base-url}") String baseUrl,
            @Value("${campusseguro.ms03.service-key}") String serviceKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Service-Key", serviceKey)
                .build();
    }

    @Override
    public void notificarAsignacion(NotificacionAsignacion notificacion) {
        restClient.post()
                .uri(RUTA_ALERTAS)
                .header("X-User-Id", notificacion.actorId())
                .header("X-User-Role", String.join(",", notificacion.actorRoles()))
                .body(construirAlerta(notificacion))
                .retrieve()
                .toBodilessEntity();
    }

    static AlertaRequest construirAlerta(NotificacionAsignacion n) {
        boolean soloSimulacros = n.actorRoles().stream()
                .map(rol -> rol.toUpperCase(Locale.ROOT))
                .noneMatch(rol -> rol.equals("COORDINADOR"));

        Origen origen = soloSimulacros
                ? new Origen("SIMULATION", UUID.fromString(n.eventoId()))
                : new Origen("MANUAL", null);
        List<String> tipos = soloSimulacros ? List.of("OTHER") : List.of("OPERATIVA");

        String instrucciones = "Se le asignó una solicitud de atención"
                + (n.prioridad() == null || n.prioridad().isBlank() ? "" : " con prioridad " + n.prioridad().toLowerCase(Locale.ROOT))
                + ". Diríjase a la ubicación indicada y consulte el panel de responsabilidades para iniciar la atención.";

        return new AlertaRequest(
                tipos,
                TITULO,
                alcance(n),
                instrucciones,
                List.of(new Destinatario("USER", n.brigadistaId())),
                origen,
                true,
                soloSimulacros ? proposito(n) : null
        );
    }

    /** El alcance es la ubicación de la solicitud; un punto de encuentro no tiene equivalente en MS03. */
    private static Alcance alcance(NotificacionAsignacion n) {
        if ("EDIFICIO".equals(n.tipoUbicacion())) {
            return new Alcance("BUILDING", List.of(n.ubicacionId()));
        }
        if ("ZONA_SEGURA".equals(n.tipoUbicacion())) {
            return new Alcance("ZONE", List.of(n.ubicacionId()));
        }
        return new Alcance("CAMPUS", null);
    }

    /** "A" + 16 caracteres de la solicitud + 15 del brigadista: único por asignación y dentro de los 32 de MS03. */
    private static String proposito(NotificacionAsignacion n) {
        String incidente = soloAlfanumerico(n.incidenteId());
        String brigadista = soloAlfanumerico(n.brigadistaId());
        String valor = "A" + recortar(incidente, 16) + recortar(brigadista, 15);
        return valor.length() > MAX_PURPOSE ? valor.substring(0, MAX_PURPOSE) : valor;
    }

    private static String soloAlfanumerico(String valor) {
        return valor == null ? "" : valor.chars()
                .filter(Character::isLetterOrDigit)
                .mapToObj(c -> String.valueOf((char) c))
                .collect(Collectors.joining());
    }

    private static String recortar(String valor, int maximo) {
        return valor.length() > maximo ? valor.substring(0, maximo) : valor;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record AlertaRequest(
            List<String> types,
            String title,
            Alcance scope,
            String instructions,
            List<Destinatario> recipients,
            Origen origin,
            Boolean activateImmediately,
            String purpose
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Alcance(String type, List<String> ids) {
    }

    record Destinatario(String type, String id) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Origen(String type, UUID simulationId) {
    }
}
