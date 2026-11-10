
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
        request.addHeader("X-User-Role", "ADMIN");

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
        request.addHeader("X-User-Role", "COORDINATOR");

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
                        authority.getAuthority().equals("ROLE_COORDINATOR")));
    }

    @Test
    void debeReconocerTodosLosRolesPermitidos() throws Exception {

        String[] roles = {
                Roles.COORDINATOR,
                Roles.BRIGADISTA,
                Roles.AUDITOR,
                Roles.SERVICE_MS06
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
}
