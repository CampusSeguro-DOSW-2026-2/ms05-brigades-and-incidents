
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.entity.Brigadista;
import edu.eci.dosw.brigades_and_incidents.exception.RecursoNoEncontradoException;
import edu.eci.dosw.brigades_and_incidents.repository.BrigadistaRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BrigadistaService {

    private final BrigadistaRepository brigadistaRepository;

    public BrigadistaService(BrigadistaRepository brigadistaRepository) {
        this.brigadistaRepository = brigadistaRepository;
    }

    // Registrar un brigadista.
    public Brigadista registrar(Brigadista brigadista) {

        if (brigadista == null) {
            throw new IllegalArgumentException(
                    "La información del brigadista es obligatoria");
        }

        return brigadistaRepository.save(brigadista);
    }

    // Consultar todos los brigadistas.
    public List<Brigadista> listarTodos() {
        return brigadistaRepository.findAll();
    }

    // Consultar un brigadista por su identificador.
    public Brigadista buscarPorId(UUID id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "El ID del brigadista es obligatorio");
        }

        return brigadistaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el brigadista con ID: " + id));
    }
}
