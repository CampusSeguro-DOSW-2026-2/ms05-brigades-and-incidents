package edu.eci.dosw.brigades_and_incidents.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientesStubTest {

    private final UsuarioClientStub usuarios = new UsuarioClientStub();
    private final BrigadistaClientStub brigadistas = new BrigadistaClientStub();
    private final UbicacionClientStub ubicaciones = new UbicacionClientStub();

    @Test
    void usuarioConocidoSeEncuentra() {
        UsuarioClient.UsuarioExterno usuario = usuarios.consultarUsuario(
                "22222222-2222-2222-2222-222222222222");

        assertThat(usuario).isNotNull();
        assertThat(usuario.nombreCompleto()).isEqualTo("Paula Novoa");
        assertThat(usuario.roles()).containsExactly("BRIGADISTA");
        assertThat(usuario.estado()).isEqualTo("ACTIVO");
    }

    @Test
    void usuarioDesconocidoDevuelveNull() {
        assertThat(usuarios.consultarUsuario("no-existe")).isNull();
    }

    @Test
    void listaDeBrigadistasTieneCuatroElementos() {
        assertThat(brigadistas.listarBrigadistas())
                .hasSize(4)
                .first()
                .satisfies(b -> {
                    assertThat(b.id()).isEqualTo("BRIG-001");
                    assertThat(b.rol()).isEqualTo("BRIGADISTA");
                });
    }

    @Test
    void ubicacionActivaSeEncuentraPorIdYTipo() {
        UbicacionClient.UbicacionExterna ubicacion =
                ubicaciones.consultarUbicacion("EDIF-001", "EDIFICIO");

        assertThat(ubicacion).isNotNull();
        assertThat(ubicacion.nombre()).isEqualTo("Edificio A");
        assertThat(ubicacion.activa()).isTrue();
    }

    @Test
    void ubicacionInactivaSeEncuentraPeroMarcadaInactiva() {
        UbicacionClient.UbicacionExterna ubicacion =
                ubicaciones.consultarUbicacion("EDIF-002", "EDIFICIO");

        assertThat(ubicacion).isNotNull();
        assertThat(ubicacion.activa()).isFalse();
    }

    @Test
    void ubicacionConTipoIncorrectoDevuelveNull() {
        assertThat(ubicaciones.consultarUbicacion("EDIF-001", "ZONA_SEGURA"))
                .isNull();
    }

    @Test
    void ubicacionDesconocidaDevuelveNull() {
        assertThat(ubicaciones.consultarUbicacion("NO-EXISTE", "EDIFICIO"))
                .isNull();
    }
}