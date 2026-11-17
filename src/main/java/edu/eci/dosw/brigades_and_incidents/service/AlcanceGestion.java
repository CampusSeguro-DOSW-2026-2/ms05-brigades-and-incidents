package edu.eci.dosw.brigades_and_incidents.service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tipo de eventos que puede gestionar quien opera el panel de brigadistas (misma regla de MS06, RN-4 y RN-5):
 * el coordinador solo eventos reales y el organizador solo simulacros. Quien tiene ambos roles gestiona los dos.
 * <p>
 * El permiso {@code ms05:brigadier:assign} sigue autorizando el endpoint; el rol (cabecera X-User-Role, que el Gateway
 * arma desde el JWT y sobrescribe) solo decide sobre qué tipo de evento se puede actuar.
 */
public record AlcanceGestion(boolean eventosReales, boolean simulacros) {

    public static final String COORDINADOR = "COORDINADOR";
    public static final String ORGANIZADOR = "ORGANIZADOR";

    public static AlcanceGestion desdeRoles(String cabeceraRoles) {
        if (cabeceraRoles == null || cabeceraRoles.isBlank()) {
            return new AlcanceGestion(false, false);
        }
        Set<String> roles = Arrays.stream(cabeceraRoles.split(","))
                .map(rol -> rol.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return new AlcanceGestion(roles.contains(COORDINADOR), roles.contains(ORGANIZADOR));
    }

    public boolean permite(boolean simulacro) {
        return simulacro ? simulacros : eventosReales;
    }

    /** Mensaje para quien intenta gestionar un evento que no le corresponde. */
    public static String mensajeNoPermitido(boolean simulacro) {
        return simulacro
                ? "Los simulacros los gestiona el organizador de simulacros."
                : "Los eventos reales los gestiona el coordinador de emergencias.";
    }
}
