package edu.eci.dosw.brigades_and_incidents.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GatewayAuthenticationFilterTest {

    private final GatewayAuthenticationFilter filter =
            new GatewayAuthenticationFilter();

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private Authentication ejecutar(MockHttpServletRequest request) throws Exception {
        SecurityContextHolder.clearContext();
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private Set<String> autoridades(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());
    }

    @Test
    void noDebeAutenticarCuandoFaltanLasCabeceras() throws Exception {
        assertNull(ejecutar(new MockHttpServletRequest()));
    }

    @Test
    void noDebeAutenticarSinPermisosValidos() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "usuario-123");
        request.addHeader("X-User-Permission", "SUPERUSUARIO,ms5:mal:formato");

        assertNull(ejecutar(request));
    }

    @Test
    void losRolesNoAutorizan() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "usuario-123");
        request.addHeader("X-User-Role", "COORDINADOR");

        assertNull(ejecutar(request));
    }

    @Test
    void debeOtorgarLosPermisosDeLaCabecera() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "usuario-123");
        request.addHeader("X-User-Permission", "ms05:brigadier:assign, ms05:incident:read,ms05:incident:read");

        Authentication authentication = ejecutar(request);

        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertEquals("usuario-123", authentication.getPrincipal());
        assertEquals(Set.of("ms05:brigadier:assign", "ms05:incident:read"), autoridades(authentication));
    }

    @Test
    void debeIgnorarLosPermisosConFormatoInvalido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "usuario-123");
        request.addHeader("X-User-Permission", "ms05:incident:read,ROLE_ADMIN,MS05:incident:read");

        Authentication authentication = ejecutar(request);

        assertNotNull(authentication);
        assertEquals(Set.of("ms05:incident:read"), autoridades(authentication));
    }

    @Test
    void permisosDeOtrosMicroserviciosTambienSeConservan() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "usuario-123");
        request.addHeader("X-User-Permission", "ms08:audit:read,ms04:checkin:write");

        assertEquals(Set.of("ms08:audit:read", "ms04:checkin:write"), autoridades(ejecutar(request)));
    }

    @Test
    void ms06SeIdentificaPorXServiceNameYNoPorLosPermisos() throws Exception {
        MockHttpServletRequest porPermiso = new MockHttpServletRequest();
        porPermiso.addHeader("X-User-Id", "usuario-123");
        porPermiso.addHeader("X-User-Permission", Permisos.SERVICIO_MS06);
        assertNull(ejecutar(porPermiso));

        MockHttpServletRequest porServicio = new MockHttpServletRequest();
        porServicio.addHeader("X-Service-Name", "ms06");
        Authentication authentication = ejecutar(porServicio);

        assertNotNull(authentication);
        assertEquals(Set.of(Permisos.SERVICIO_MS06), autoridades(authentication));
    }
}
