package edu.eci.dosw.brigades_and_incidents.support;

import edu.eci.dosw.brigades_and_incidents.security.Permisos;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Permisos de MS05 que la Matriz Rol-Permiso asigna inicialmente a cada rol. Sirve para armar la cabecera
 * X-User-Permission y el contexto de seguridad de las pruebas sin repetir listas en cada una.
 */
public final class PermisosPrueba {

    private static final List<String> USUARIO_GENERAL = List.of(Permisos.INCIDENTE_REPORTAR);

    private static final Map<String, List<String>> POR_ROL = Map.of(
            "ESTUDIANTE", USUARIO_GENERAL,
            "PROFESOR", USUARIO_GENERAL,
            "ADMINISTRATIVO", USUARIO_GENERAL,
            "ADMIN", USUARIO_GENERAL,
            "BRIGADISTA", List.of(
                    Permisos.PANEL_VER_PROPIO, Permisos.ASISTENCIA_VER, Permisos.ASISTENCIA_REGISTRAR,
                    Permisos.ASISTENCIA_ACTUALIZAR_ESTADO, Permisos.INCIDENTE_REPORTAR,
                    Permisos.INCIDENTE_VER, Permisos.INCIDENTE_ACTUALIZAR),
            "COORDINADOR", List.of(
                    Permisos.BRIGADISTA_ASIGNAR, Permisos.ASISTENCIA_VER, Permisos.ASISTENCIA_REGISTRAR,
                    Permisos.ASISTENCIA_ASIGNAR, Permisos.ASISTENCIA_ACTUALIZAR_ESTADO,
                    Permisos.INCIDENTE_REPORTAR, Permisos.INCIDENTE_VER, Permisos.INCIDENTE_ACTUALIZAR),
            "ORGANIZADOR", List.of(
                    Permisos.BRIGADISTA_ASIGNAR, Permisos.INCIDENTE_REPORTAR, Permisos.INCIDENTE_VER),
            "AUDITOR", List.of(Permisos.INCIDENTE_VER, Permisos.AUDITORIA_VER));

    private PermisosPrueba() {
        // Utility class.
    }

    /** Permisos efectivos de uno o varios roles (separados por coma), sin repetidos. */
    public static Set<String> de(String roles) {
        return Arrays.stream(roles.split(","))
                .map(rol -> rol.trim().toUpperCase())
                .flatMap(rol -> POR_ROL.getOrDefault(rol, List.of()).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Valor de la cabecera X-User-Permission para esos roles. */
    public static String cabecera(String roles) {
        return String.join(",", de(roles));
    }

    /** Fija en el contexto de seguridad al usuario con los permisos de esos roles (para pruebas de servicio). */
    public static void autenticarComo(String roles) {
        autenticarConPermisos(de(roles));
    }

    public static void autenticarConPermisos(Set<String> permisos) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "usuario-prueba", null,
                permisos.stream().map(SimpleGrantedAuthority::new).toList()));
    }

    public static void limpiar() {
        SecurityContextHolder.clearContext();
    }
}
