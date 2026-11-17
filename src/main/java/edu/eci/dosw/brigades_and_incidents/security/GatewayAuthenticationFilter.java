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
 *   <li>X-User-Id y X-User-Permission: el usuario y sus permisos efectivos (la unión de los de sus roles),
 *       separados por coma. Solo cuentan los de formato válido; el resto se ignora. MS05 autoriza únicamente con
 *       permisos. X-User-Role no autoriza nada: solo lo usa el panel de gestión de brigadistas para decidir el tipo
 *       de evento (el coordinador, reales; el organizador, simulacros; ver AlcanceGestion).</li>
 *   <li>X-Service-Name: el servicio que llama con su clave (ms01 … ms08). El Gateway la deriva de la clave,
 *       nadie más puede fijarla; ms06 recibe la autoridad {@link Permisos#SERVICIO_MS06}.</li>
 * </ul>
 */
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    static final String USER_ID_HEADER = "X-User-Id";
    static final String USER_PERMISSION_HEADER = "X-User-Permission";
    static final String SERVICE_NAME_HEADER = "X-Service-Name";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String userId = request.getHeader(USER_ID_HEADER);
        String userPermissions = request.getHeader(USER_PERMISSION_HEADER);
        String servicio = request.getHeader(SERVICE_NAME_HEADER);

        List<GrantedAuthority> autoridades = new ArrayList<>();
        boolean usuario = userId != null && !userId.isBlank() && userPermissions != null && !userPermissions.isBlank();
        if (usuario) {
            Arrays.stream(userPermissions.split(","))
                    .map(String::trim)
                    .filter(permiso -> Permisos.FORMATO.matcher(permiso).matches())
                    .distinct()
                    .forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(permiso)));
        }
        boolean llamadaMs06 = servicio != null && "ms06".equalsIgnoreCase(servicio.trim());
        if (llamadaMs06) {
            autoridades.add(new SimpleGrantedAuthority(Permisos.SERVICIO_MS06));
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
