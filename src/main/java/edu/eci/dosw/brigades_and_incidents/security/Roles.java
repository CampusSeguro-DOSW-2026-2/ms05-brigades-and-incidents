
package edu.eci.dosw.brigades_and_incidents.security;

/**
 * Centralized role definitions used by Spring Security.
 */
public final class Roles {

    public static final String COORDINATOR = "COORDINATOR";
    public static final String BRIGADISTA = "BRIGADISTA";
    public static final String AUDITOR = "AUDITOR";
    public static final String SERVICE_MS06 = "SERVICE_MS06";

    private Roles() {
        // Utility class.
    }
}
