
package edu.eci.dosw.brigades_and_incidents.security;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

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
                            response.sendError(
                                    HttpServletResponse.SC_UNAUTHORIZED,
                                    "Se requiere autenticacion"
                            )
                    )
                    .accessDeniedHandler((request, response, exception) ->
                            response.sendError(
                                    HttpServletResponse.SC_FORBIDDEN,
                                    "No tiene permisos para realizar esta operacion"
                            )
                    )
            )

            .authorizeHttpRequests(auth -> auth
                    .anyRequest().authenticated()
            )

            .addFilterBefore(
                    new GatewayAuthenticationFilter(),
                    UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}
