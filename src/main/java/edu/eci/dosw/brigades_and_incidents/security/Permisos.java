package edu.eci.dosw.brigades_and_incidents.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.regex.Pattern;

/**
 * Permisos que MS05 exige (Matriz Rol-Permiso y Estándares §4). El Gateway propaga los permisos efectivos del
 * usuario en X-User-Permission; MS05 autoriza solo con ellos y no consulta los roles.
 */
public final class Permisos {

    /** Asignar brigadistas a edificios, zonas o puntos de encuentro (COORDINADOR, ORGANIZADOR). */
    public static final String BRIGADISTA_ASIGNAR = "ms05:brigadier:assign";
    /** Ver las zonas y tareas propias (BRIGADISTA). */
    public static final String PANEL_VER_PROPIO = "ms05:panel:read_own";
    public static final String ASISTENCIA_VER = "ms05:assistance:read";
    public static final String ASISTENCIA_REGISTRAR = "ms05:assistance:register";
    /** Asignar una solicitud a un brigadista y cerrarla (COORDINADOR). */
    public static final String ASISTENCIA_ASIGNAR = "ms05:assistance:assign";
    /** Cambiar el estado de atención: en atención, resuelta (BRIGADISTA, COORDINADOR). */
    public static final String ASISTENCIA_ACTUALIZAR_ESTADO = "ms05:assistance:update_status";
    public static final String INCIDENTE_REPORTAR = "ms05:incident:report";
    public static final String INCIDENTE_VER = "ms05:incident:read";
    public static final String INCIDENTE_ACTUALIZAR = "ms05:incident:update";
    /** Consulta de la auditoría (AUDITOR); la auditoría es de MS08. */
    public static final String AUDITORIA_VER = "ms08:audit:read";

    /**
     * Autoridad de las llamadas de MS06. No llega en X-User-Permission: el filtro la otorga cuando el Gateway
     * envía X-Service-Name: ms06 (Estándares §5).
     */
    public static final String SERVICIO_MS06 = "SERVICE_MS06";

    /** Formato de un permiso (Estándares §4): ms<dos dígitos>:<recurso>:<acción>. */
    public static final Pattern FORMATO = Pattern.compile("^ms[0-9]{2}:[a-z][a-z0-9_]*:[a-z][a-z0-9_]*$");

    private Permisos() {
        // Utility class.
    }

    /** Indica si el usuario autenticado de la solicitud actual tiene el permiso. */
    public static boolean tiene(String permiso) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || permiso == null) {
            return false;
        }
        for (GrantedAuthority autoridad : authentication.getAuthorities()) {
            if (permiso.equals(autoridad.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    /** Indica si tiene al menos uno de los permisos. */
    public static boolean tieneAlguno(String... permisos) {
        for (String permiso : permisos) {
            if (tiene(permiso)) {
                return true;
            }
        }
        return false;
    }

    /** Quien gestiona asignaciones e incidentes de todo el evento (coordinador u organizador). */
    public static boolean esGestor() {
        return tieneAlguno(BRIGADISTA_ASIGNAR, ASISTENCIA_ASIGNAR);
    }
}
