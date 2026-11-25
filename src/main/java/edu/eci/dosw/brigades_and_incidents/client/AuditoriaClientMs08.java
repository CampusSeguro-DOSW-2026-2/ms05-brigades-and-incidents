package edu.eci.dosw.brigades_and_incidents.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.eci.dosw.brigades_and_incidents.exception.ErrorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Envía hechos de auditoría de MS05 hacia MS08 a través del API Gateway.
 * Ruta interna: {@code POST /api/v1/audit/events}.
 * Autenticación servicio a servicio: {@code X-Service-Key} (el Gateway la valida e inyecta
 * {@code X-Service-Name: ms05} hacia MS08).
 * Propaga {@code X-Correlation-ID} tanto en cabecera como en el cuerpo del hecho.
 */
@Component
@Profile("!stub")
public class AuditoriaClientMs08 implements AuditoriaClient {

    static final String RUTA_AUDITORIA = "/api/v1/audit/events";
    private static final String MODULO_MS05 = "MS05";
    private static final int MAX_TEXTO = 100;
    private static final int MAX_ATRIBUTOS = 20;

    private static final Pattern PATRON_UUID = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    private static final Pattern PATRON_RECURSO = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*:\\S{1,100}$");
    private static final Pattern PATRON_CLAVE_ATRIBUTO = Pattern.compile("^[a-z][a-z0-9_]*$");

    private final RestClient restClient;

    public AuditoriaClientMs08(
            RestClient.Builder builder,
            @Value("${campusseguro.ms08.base-url}") String baseUrl,
            @Value("${campusseguro.ms08.service-key:}") String serviceKey
    ) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("X-Service-Key", serviceKey)
                .build();
    }

    @Override
    public void registrarEvento(EventoAuditoria evento) {
        if (evento == null) {
            return;
        }
        String correlationId = obtenerCorrelationId();
        AuditEventPayload payload = construirPayload(evento, correlationId);

        restClient.post()
                .uri(RUTA_AUDITORIA)
                .header(ErrorResponse.CORRELATION_HEADER, correlationId)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    static AuditEventPayload construirPayload(EventoAuditoria evento, String correlationId) {
        String corr = normalizarTexto(correlationId, MAX_TEXTO);
        if (corr == null) {
            corr = UUID.randomUUID().toString();
        }

        String eventoId = normalizarUuid(evento.eventoId());
        String tipoEvento = eventoId == null
                ? null
                : (Boolean.TRUE.equals(evento.esSimulacro()) ? "SIMULACRO" : "REAL");

        return new AuditEventPayload(
                UUID.randomUUID().toString(),
                normalizarUuid(evento.usuarioId()),
                MODULO_MS05,
                normalizarAccion(evento.accion()),
                normalizarRecurso(evento.recursoTipo(), evento.recursoId()),
                normalizarResultado(evento.resultado()),
                evento.fechaHora() != null ? evento.fechaHora() : OffsetDateTime.now(),
                corr,
                normalizarTexto(evento.detalle(), MAX_TEXTO),
                eventoId,
                tipoEvento,
                sanitizarAtributos(evento.atributos())
        );
    }

    private static String obtenerCorrelationId() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos instanceof ServletRequestAttributes servlet) {
            String recibido = servlet.getRequest().getHeader(ErrorResponse.CORRELATION_HEADER);
            if (recibido != null && !recibido.isBlank()) {
                return recortar(recibido.trim(), MAX_TEXTO);
            }
        }
        return UUID.randomUUID().toString();
    }

    private static String normalizarUuid(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return PATRON_UUID.matcher(limpio).matches() ? limpio : null;
    }

    private static String normalizarAccion(String accion) {
        if (accion == null || accion.isBlank()) {
            return "ACCION_DESCONOCIDA";
        }
        String limpia = accion.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9_]+", "_")
                .replaceAll("^_+|_+$", "");
        if (limpia.isEmpty()) {
            return "ACCION_DESCONOCIDA";
        }
        if (limpia.charAt(0) < 'A' || limpia.charAt(0) > 'Z') {
            limpia = "A_" + limpia;
        }
        return recortar(limpia, MAX_TEXTO);
    }

    private static String normalizarRecurso(String tipo, String id) {
        if (tipo == null || tipo.isBlank() || id == null || id.isBlank()) {
            return null;
        }
        String tipoLimpio = tipo.trim().replaceAll("[^A-Za-z0-9_]+", "_");
        String idLimpio = recortar(id.trim().replaceAll("\\s+", "_"), MAX_TEXTO);
        String recurso = tipoLimpio + ":" + idLimpio;
        return PATRON_RECURSO.matcher(recurso).matches() ? recurso : null;
    }

    private static String normalizarResultado(String resultado) {
        if (resultado == null) {
            return "EXITO";
        }
        String normalizado = resultado.trim().toUpperCase(Locale.ROOT);
        if ("EXITO".equals(normalizado) || "EXITOSO".equals(normalizado)) {
            return "EXITO";
        }
        return "FALLO";
    }

    private static Map<String, Object> sanitizarAtributos(Map<String, Object> atributos) {
        if (atributos == null || atributos.isEmpty()) {
            return null;
        }
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entrada : atributos.entrySet()) {
            if (resultado.size() >= MAX_ATRIBUTOS) {
                break;
            }
            String clave = entrada.getKey();
            Object valor = entrada.getValue();
            if (clave == null || !PATRON_CLAVE_ATRIBUTO.matcher(clave).matches() || valor == null) {
                continue;
            }
            if (valor instanceof Number || valor instanceof Boolean) {
                resultado.put(clave, valor);
            } else if (valor instanceof String texto && !texto.isBlank()) {
                resultado.put(clave, recortar(texto.trim(), MAX_TEXTO));
            }
        }
        return resultado.isEmpty() ? null : resultado;
    }

    private static String normalizarTexto(String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return recortar(valor.trim(), maximo);
    }

    private static String recortar(String valor, int maximo) {
        return valor.length() > maximo ? valor.substring(0, maximo) : valor;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record AuditEventPayload(
            String id,
            @JsonProperty("usuario_id") String usuarioId,
            String modulo,
            String accion,
            @JsonProperty("recurso_afectado") String recursoAfectado,
            String resultado,
            @JsonProperty("fecha_hora") OffsetDateTime fechaHora,
            @JsonProperty("correlation_id") String correlationId,
            String detalle,
            @JsonProperty("evento_id") String eventoId,
            @JsonProperty("tipo_evento") String tipoEvento,
            Map<String, Object> atributos
    ) {
        AuditEventPayload {
            atributos = atributos == null || atributos.isEmpty()
                    ? null
                    : Collections.unmodifiableMap(new LinkedHashMap<>(atributos));
        }
    }
}