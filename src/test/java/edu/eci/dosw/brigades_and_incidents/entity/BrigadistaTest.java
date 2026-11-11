package edu.eci.dosw.brigades_and_incidents.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BrigadistaTest {

    @Test
    void debeCrearBrigadistaConConstructorVacio() {
        Brigadista brigadista = new Brigadista();

        assertNull(brigadista.getId());
        assertNull(brigadista.getUsuarioIdExterno());
        assertNull(brigadista.getEstado());
    }

    @Test
    void debeCrearBrigadistaConUsuarioYEstado() {
        EstadoBrigadista estado =
                new EstadoBrigadista("ACTIVO", "Brigadista activo");

        Brigadista brigadista =
                new Brigadista("usuario-123", estado);

        assertEquals("usuario-123", brigadista.getUsuarioIdExterno());
        assertSame(estado, brigadista.getEstado());
    }

    @Test
    void debePermitirModificarUsuarioYEstado() {
        Brigadista brigadista = new Brigadista();

        EstadoBrigadista estado =
                new EstadoBrigadista("INACTIVO", "Brigadista inactivo");

        brigadista.setUsuarioIdExterno("usuario-456");
        brigadista.setEstado(estado);

        assertEquals("usuario-456", brigadista.getUsuarioIdExterno());
        assertSame(estado, brigadista.getEstado());
    }
}