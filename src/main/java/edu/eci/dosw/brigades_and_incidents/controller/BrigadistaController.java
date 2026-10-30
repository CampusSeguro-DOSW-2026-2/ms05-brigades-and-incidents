
package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.entity.Brigadista;
import edu.eci.dosw.brigades_and_incidents.service.BrigadistaService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/brigadistas")
public class BrigadistaController {

    private final BrigadistaService brigadistaService;

    public BrigadistaController(BrigadistaService brigadistaService) {
        this.brigadistaService = brigadistaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Brigadista registrar(@RequestBody Brigadista brigadista) {
        return brigadistaService.registrar(brigadista);
    }

    @GetMapping
    public List<Brigadista> listarTodos() {
        return brigadistaService.listarTodos();
    }

    @GetMapping("/{id}")
    public Brigadista buscarPorId(@PathVariable UUID id) {
        return brigadistaService.buscarPorId(id);
    }
}
