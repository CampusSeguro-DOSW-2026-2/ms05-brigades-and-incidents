
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.entity.ConfiguracionAsignacion;
import edu.eci.dosw.brigades_and_incidents.enums.TipoUbicacion;
import edu.eci.dosw.brigades_and_incidents.exception.ConflictoOperacionException;
import edu.eci.dosw.brigades_and_incidents.exception.RecursoNoEncontradoException;
import edu.eci.dosw.brigades_and_incidents.repository.ConfiguracionAsignacionRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ConfiguracionAsignacionService {

    private final ConfiguracionAsignacionRepository configuracionRepository;

    public ConfiguracionAsignacionService(
            ConfiguracionAsignacionRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    // Registrar una configuración global por tipo de ubicación.
    public ConfiguracionAsignacion registrar(
            ConfiguracionAsignacion configuracion) {

        if (configuracion == null || configuracion.getTipoUbicacion() == null) {
            throw new IllegalArgumentException(
                    "El tipo de ubicación es obligatorio");
        }

        if (configuracion.getCantidadBrigadistas() == null
                || configuracion.getCantidadBrigadistas() <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad de brigadistas debe ser mayor que cero");
        }

        boolean existe = configuracionRepository
                .existsByTipoUbicacion(configuracion.getTipoUbicacion());

        if (existe) {
            throw new ConflictoOperacionException(
                    "Ya existe una configuración para este tipo de ubicación");
        }

        return configuracionRepository.save(configuracion);
    }

    // Consultar todas las configuraciones.
    public List<ConfiguracionAsignacion> listarTodas() {
        return configuracionRepository.findAll();
    }

    // Consultar una configuración por tipo de ubicación.
    public ConfiguracionAsignacion consultarPorTipoUbicacion(
            TipoUbicacion tipoUbicacion) {

        if (tipoUbicacion == null) {
            throw new IllegalArgumentException(
                    "El tipo de ubicación es obligatorio");
        }

        return configuracionRepository.findByTipoUbicacion(tipoUbicacion)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una configuración para el tipo de ubicación: "
                                + tipoUbicacion));
    }

    // Consultar una configuración por su identificador.
    public ConfiguracionAsignacion consultarPorId(UUID id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "El ID de la configuración es obligatorio");
        }

        return configuracionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la configuración con ID: " + id));
    }
}
