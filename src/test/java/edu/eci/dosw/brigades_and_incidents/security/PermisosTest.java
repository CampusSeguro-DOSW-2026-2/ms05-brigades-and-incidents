package edu.eci.dosw.brigades_and_incidents.security;

import edu.eci.dosw.brigades_and_incidents.support.PermisosPrueba;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PermisosTest {

    @AfterEach
    void limpiar() {
        PermisosPrueba.limpiar();
    }

    @Test
    void sinUsuarioAutenticadoNoTienePermisos() {
        PermisosPrueba.limpiar();

        assertFalse(Permisos.tiene(Permisos.INCIDENTE_VER));
        assertFalse(Permisos.tiene(null));
        assertFalse(Permisos.esGestor());
    }

    @Test
    void tieneSoloLosPermisosDelUsuario() {
        PermisosPrueba.autenticarConPermisos(Set.of(Permisos.INCIDENTE_VER));

        assertTrue(Permisos.tiene(Permisos.INCIDENTE_VER));
        assertFalse(Permisos.tiene(Permisos.INCIDENTE_ACTUALIZAR));
        assertTrue(Permisos.tieneAlguno(Permisos.INCIDENTE_ACTUALIZAR, Permisos.INCIDENTE_VER));
        assertFalse(Permisos.tieneAlguno(Permisos.INCIDENTE_ACTUALIZAR, Permisos.PANEL_VER_PROPIO));
    }

    @Test
    void esGestorConCualquieraDeLosPermisosDeCoordinacion() {
        PermisosPrueba.autenticarComo("ORGANIZADOR");
        assertTrue(Permisos.esGestor());

        PermisosPrueba.autenticarComo("COORDINADOR");
        assertTrue(Permisos.esGestor());

        PermisosPrueba.autenticarComo("BRIGADISTA");
        assertFalse(Permisos.esGestor());

        PermisosPrueba.autenticarComo("AUDITOR");
        assertFalse(Permisos.esGestor());
    }

    @Test
    void reconocePermisosConFormatoValido() {
        assertTrue(Permisos.FORMATO.matcher("ms05:incident:read").matches());
        assertTrue(Permisos.FORMATO.matcher("ms01:role:read_own").matches());
        assertFalse(Permisos.FORMATO.matcher("ROLE_ADMIN").matches());
        assertFalse(Permisos.FORMATO.matcher("ms5:incident:read").matches());
        assertTrue(Permisos.FORMATO.matcher("ms08:audit_drill:read").matches());
        assertFalse(Permisos.FORMATO.matcher("ms08:audit:drill:read").matches());
    }
}
