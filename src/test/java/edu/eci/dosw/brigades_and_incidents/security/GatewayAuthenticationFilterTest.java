
package edu.eci.dosw.brigades_and_incidents.security;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

class GatewayAuthenticationFilterTest {

    private final GatewayAuthenticationFilter filter =
            new GatewayAuthenticationFilter();

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noDebeAutenticarCuandoFaltanLosEncabezados() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertNull(authentication);
    }

    @Test
    void noDebeAutenticarCuandoElRolEsInvalido() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "usuario-123");
        request.addHeader("X-User-Role", "SUPERUSUARIO");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertNull(authentication);
    }

    @Test
    void debeAutenticarConRolValido() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "usuario-123");
        request.addHeader("X-User-Role", "COORDINADOR");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertEquals("usuario-123", authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_COORDINADOR")));
    }

    @Test
    void debeReconocerTodosLosRolesPermitidos() throws Exception {

        String[] roles = {
                Roles.COORDINADOR,
                Roles.BRIGADISTA,
                Roles.AUDITOR,
                "ESTUDIANTE",
                "ADMIN"
        };

        for (String role : roles) {

            SecurityContextHolder.clearContext();

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-User-Id", "usuario-123");
            request.addHeader("X-User-Role", role);

            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            assertNotNull(authentication, "No se autenticó el rol: " + role);

            assertTrue(authentication.getAuthorities().stream()
                    .anyMatch(authority ->
                            authority.getAuthority().equals("ROLE_" + role)));
        }
    }

    @Test
    void debeOtorgarTodosLosRolesDeUnaCabeceraConVariosRoles() throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "usuario-123");
        request.addHeader("X-User-Role", "BRIGADISTA,ESTUDIANTE");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_BRIGADISTA")));
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ESTUDIANTE")));
    }

    @Test
    void ms06SeIdentificaPorXServiceNameYNoPorElRol() throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest porRol = new MockHttpServletRequest();
        porRol.addHeader("X-User-Id", "usuario-123");
        porRol.addHeader("X-User-Role", Roles.SERVICE_MS06);

        filter.doFilter(porRol, new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        SecurityContextHolder.clearContext();
        MockHttpServletRequest porServicio = new MockHttpServletRequest();
        porServicio.addHeader("X-Service-Name", "ms06");

        filter.doFilter(porServicio, new MockHttpServletResponse(), new MockFilterChain());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + Roles.SERVICE_MS06)));
    }
}
