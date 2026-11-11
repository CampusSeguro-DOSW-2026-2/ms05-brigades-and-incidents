
package edu.eci.dosw.brigades_and_incidents.client;

import java.util.List;

public interface UsuarioClient {

    UsuarioExterno consultarUsuario(String usuarioId);

    record UsuarioExterno(
            String id,
            String nombreCompleto,
            List<String> roles,
            String estado
    ) {

        public UsuarioExterno {
            roles = roles == null ? List.of() : List.copyOf(roles);
        }
    }
}
