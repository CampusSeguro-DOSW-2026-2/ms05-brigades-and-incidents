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

    /** Asignar brigadistas a edificios, zonas o puntos de encuentro en simulacros (ORGANIZADOR). */
    public static final String BRIGADISTA_ASIGNAR_SIMULACRO = "ms05:brigadier:assign_to_drill";
    /** Asignar brigadistas a edificios, zonas o puntos de encuentro en eventos reales (COORDINADOR). */
    public static final String BRIGADISTA_ASIGNAR_REAL = "ms05:brigadier:assign_to_real";
    /** Ver las zonas y tareas propias (BRIGADISTA). */
    public static final String PANEL_VER_PROPIO = "ms05:panel:read_own";
    public static final String ASISTENCIA_VER = "ms05:assistance:read";
    public static final String ASISTENCIA_REGISTRAR = "ms05:assistance:register";
    /** Asignar una solicitud a un brigadista y cerrarla en simulacros (ORGANIZADOR). */
    public static final String ASISTENCIA_ASIGNAR_SIMULACRO = "ms05:assistance:assign_to_drill";
    /** Asignar una solicitud a un brigadista y cerrarla en eventos reales (COORDINADOR). */
    public static final String ASISTENCIA_ASIGNAR_REAL = "ms05:assistance:assign_to_real";
    /** Cambiar el estado de atención: en atención, resuelta (BRIGADISTA, COORDINADOR). */
    public static final String ASISTENCIA_ACTUALIZAR_ESTADO = "ms05:assistance:update_status";
    public static final String INCIDENTE_REPORTAR = "ms05:incident:report";
    public static final String INCIDENTE_VER = "ms05:incident:read";
    /** Consultar, editar y eliminar los incidentes propios (toda la comunidad, Matriz Rol-Permiso). */
    public static final String INCIDENTE_GESTIONAR_PROPIO = "ms05:incident:manage_own";
    public static final String INCIDENTE_ACTUALIZAR = "ms05:incident:update";
    /** Consulta de la auditoría de simulacros (ORGANIZADOR, ADMIN, AUDITOR); el permiso es de MS08. */
    public static final String AUDITORIA_SIMULACRO_VER = "ms08:audit_drill:read";
    /** Consulta de la auditoría de eventos reales (COORDINADOR, ADMIN, AUDITOR); el permiso es de MS08. */
    public static final String AUDITORIA_REAL_VER = "ms08:audit_real_event:read";

    /**
     * Autoridad de las llamadas de MS06. No llega en X-User-Permission: el filtro la otorga cuando el Gateway
     * envía X-Service-Name: ms06 (Estándares §5).
     */
    public static final String SERVICIO_MS06 = "SERVICE_MS06";

    /**
     * Formato de un permiso (Estándares §4): ms<dos dígitos>:<recurso>:<acción>, siempre tres segmentos, igual que
     * el Gateway (que rechaza el token si trae otro formato). Por eso los de MS08 son ms08:audit_drill:read y no
     * ms08:audit:drill:read.
     */
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

    /**
     * Quien gestiona asignaciones e incidentes de todo el evento: cualquiera de los permisos de asignación, de
     * simulacros (organizador) o de eventos reales (coordinador).
     */
    public static boolean esGestor() {
        return tieneAlguno(BRIGADISTA_ASIGNAR_SIMULACRO, BRIGADISTA_ASIGNAR_REAL,
                ASISTENCIA_ASIGNAR_SIMULACRO, ASISTENCIA_ASIGNAR_REAL);
    }

    /**
     * Indica si tiene el permiso que corresponde al tipo del evento: el de simulacros o el de eventos reales. Si no se
     * conoce el tipo (el evento aún no está registrado en MS05), basta con cualquiera de los dos.
     *
     * @param simulacro true si el evento es un simulacro, false si es real, null si no se conoce
     */
    public static boolean tieneParaEvento(Boolean simulacro, String permisoSimulacro, String permisoReal) {
        if (simulacro == null) {
            return tieneAlguno(permisoSimulacro, permisoReal);
        }
        return tiene(simulacro ? permisoSimulacro : permisoReal);
    }
}
