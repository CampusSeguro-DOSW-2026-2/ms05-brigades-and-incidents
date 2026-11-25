package edu.eci.dosw.brigades_and_incidents.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Cliente HTTP de las llamadas a otros servicios, siempre por el API Gateway (Estándares §5):
 * <ul>
 *   <li>HTTP/1.1: el cliente del JDK intenta pasar a HTTP/2 por defecto y, al enviar un cuerpo, esa negociación deja
 *       la petición colgada contra el Gateway (así lo resolvieron también MS04 y MS06);</li>
 *   <li>timeout de conexión y de lectura de 2000 ms, para que un servicio lento no bloquee la operación en curso.</li>
 * </ul>
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder(@Value("${campusseguro.http.timeout-ms:2000}") long timeoutMs) {
        Duration timeout = Duration.ofMillis(timeoutMs);
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder().requestFactory(requestFactory);
    }
}
