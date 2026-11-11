package edu.eci.dosw.brigades_and_incidents.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Autentica con las cabeceras que inyecta el Gateway (Estándares §4 y §5):
 * <ul>
 *   <li>X-User-Id y X-User-Role: el usuario y sus roles, separados por coma. Solo cuentan los del
 *       catálogo; el resto se ignora.</li>
 *   <li>X-Service-Name: el servicio que llama con su clave (ms01 … ms08). El Gateway la deriva de la clave,
 *       nadie más puede fijarla; ms06 recibe la autoridad {@link Roles#SERVICE_MS06}.</li>
 * </ul>
 */
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    static final String USER_ID_HEADER = "X-User-Id";
    static final String USER_ROLE_HEADER = "X-User-Role";
    static final String SERVICE_NAME_HEADER = "X-Service-Name";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String userId = request.getHeader(USER_ID_HEADER);
        String userRoles = request.getHeader(USER_ROLE_HEADER);
        String servicio = request.getHeader(SERVICE_NAME_HEADER);

        List<GrantedAuthority> autoridades = new ArrayList<>();
        boolean usuario = userId != null && !userId.isBlank() && userRoles != null && !userRoles.isBlank();
        if (usuario) {
            Arrays.stream(userRoles.split(","))
                    .map(rol -> rol.trim().toUpperCase(Locale.ROOT))
                    .filter(Roles.CATALOGO::contains)
                    .distinct()
                    .forEach(rol -> autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol)));
        }
        boolean llamadaMs06 = servicio != null && "ms06".equalsIgnoreCase(servicio.trim());
        if (llamadaMs06) {
            autoridades.add(new SimpleGrantedAuthority("ROLE_" + Roles.SERVICE_MS06));
        }

        if (!autoridades.isEmpty()) {
            String principal = usuario ? userId.trim() : "servicio:" + servicio.trim().toLowerCase(Locale.ROOT);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, autoridades);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        }

        filterChain.doFilter(request, response);
    }
}
