
package edu.eci.dosw.brigades_and_incidents.security;

import edu.eci.dosw.brigades_and_incidents.exception.ErrorResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "JsonMapper es el bean compartido de Spring y es seguro entre hilos."
)
public class SecurityConfig {

    private final JsonMapper jsonMapper;

    public SecurityConfig(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .formLogin(form -> form.disable())

            .httpBasic(basic -> basic.disable())

            .logout(logout -> logout.disable())

            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, exception) ->
                            escribirError(response, HttpStatus.UNAUTHORIZED, "Se requiere autenticación.")
                    )
                    .accessDeniedHandler((request, response, exception) ->
                            escribirError(response, HttpStatus.FORBIDDEN,
                                    "No tiene permisos para realizar esta operación.")
                    )
            )
            .authorizeHttpRequests(auth -> auth
                    // Health para el borde y el despliegue (Estándares §11); el Gateway no lo enruta.
                    .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                    .anyRequest().authenticated()
            )
            .addFilterBefore(
                    new GatewayAuthenticationFilter(),
                    UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    /** 401/403 de la cadena de seguridad con el ErrorResponse de los Estándares (§7.1). */
    private void escribirError(HttpServletResponse response, HttpStatus status, String mensaje)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(jsonMapper.writeValueAsString(ErrorResponse.de(status, mensaje)));
    }
}
