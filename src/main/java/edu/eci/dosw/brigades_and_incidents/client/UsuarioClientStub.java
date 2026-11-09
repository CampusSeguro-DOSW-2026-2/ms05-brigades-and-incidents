package edu.eci.dosw.brigades_and_incidents.client;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Profile("stub")
public class UsuarioClientStub implements UsuarioClient {

    private final Map<String, UsuarioExterno> usuarios = Map.of(
            "11111111-1111-1111-1111-111111111111",
            new UsuarioExterno(
                    "11111111-1111-1111-1111-111111111111",
                    "Daniel García",
                    List.of("ESTUDIANTE"),
                    "ACTIVO"
            ),
            "22222222-2222-2222-2222-222222222222",
            new UsuarioExterno(
                    "22222222-2222-2222-2222-222222222222",
                    "Paula Novoa",
                    List.of("BRIGADISTA"),
                    "ACTIVO"
            ),
            "33333333-3333-3333-3333-333333333333",
            new UsuarioExterno(
                    "33333333-3333-3333-3333-333333333333",
                    "Derly Pachón",
                    List.of("PROFESOR"),
                    "ACTIVO"
            )
    );

    @Override
    public UsuarioExterno consultarUsuario(String usuarioId) {
        return usuarios.get(usuarioId);
    }
}
