package edu.eci.dosw.brigades_and_incidents.security;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * Roles de CampusSeguro (Estándares §4 y Matriz Rol-Permiso) y el rol técnico que identifica a MS06.
 */
public final class Roles {

    public static final String COORDINADOR = "COORDINADOR";
    public static final String BRIGADISTA = "BRIGADISTA";
    public static final String AUDITOR = "AUDITOR";

    /**
     * Autoridad de las llamadas de MS06. No llega en X-User-Role: el filtro la otorga cuando el Gateway
     * envía X-Service-Name: ms06 (Estándares §5).
     */
    public static final String SERVICE_MS06 = "SERVICE_MS06";

    /** Catálogo de roles que el Gateway propaga en X-User-Role. */
    public static final Set<String> CATALOGO = Set.of(
            "ESTUDIANTE", "PROFESOR", "ADMINISTRATIVO", BRIGADISTA,
            COORDINADOR, "ORGANIZADOR", "ADMIN", AUDITOR);

    private Roles() {
        // Utility class.
    }

    /**
     * Indica si la cabecera X-User-Role (roles separados por coma, Estándares §4) incluye el rol.
     */
    public static boolean tiene(String cabeceraRoles, String rol) {
        if (cabeceraRoles == null || rol == null) {
            return false;
        }
        return Arrays.stream(cabeceraRoles.split(","))
                .map(valor -> valor.trim().toUpperCase(Locale.ROOT))
                .anyMatch(rol::equalsIgnoreCase);
    }
}
