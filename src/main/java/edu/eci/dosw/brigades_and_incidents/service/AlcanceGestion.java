package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.security.Permisos;

/**
 * Tipo de eventos que puede gestionar quien opera el panel de brigadistas, según sus permisos (Matriz Rol-Permiso,
 * MR-07): {@code ms05:brigadier:assign_to_real} para los eventos reales y {@code ms05:brigadier:assign_to_drill} para
 * los simulacros. Quien tiene los dos gestiona ambos. No se decide por el nombre del rol.
 */
public record AlcanceGestion(boolean eventosReales, boolean simulacros) {

    /** Alcance del usuario autenticado en la solicitud actual, a partir de sus permisos. */
    public static AlcanceGestion desdePermisos() {
        return new AlcanceGestion(
                Permisos.tiene(Permisos.BRIGADISTA_ASIGNAR_REAL),
                Permisos.tiene(Permisos.BRIGADISTA_ASIGNAR_SIMULACRO)
        );
    }

    public boolean permite(boolean simulacro) {
        return simulacro ? simulacros : eventosReales;
    }

    /** Mensaje para quien intenta gestionar un evento que no le corresponde. */
    public static String mensajeNoPermitido(boolean simulacro) {
        return simulacro
                ? "No tiene permiso para gestionar brigadistas en simulacros."
                : "No tiene permiso para gestionar brigadistas en eventos reales.";
    }
}
