
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.client.BrigadistaClient;
import edu.eci.dosw.brigades_and_incidents.client.BrigadistaClient.BrigadistaExterno;
import edu.eci.dosw.brigades_and_incidents.dto.AsignacionBrigadistaResumenResponse;
import edu.eci.dosw.brigades_and_incidents.dto.BusquedaBrigadistasResponse;
import edu.eci.dosw.brigades_and_incidents.dto.BuscarBrigadistaResponse;
import edu.eci.dosw.brigades_and_incidents.entity.AsignacionBrigadista;
import edu.eci.dosw.brigades_and_incidents.entity.Brigadista;
import edu.eci.dosw.brigades_and_incidents.entity.EstadoBrigadista;
import edu.eci.dosw.brigades_and_incidents.exception.Ms01IntegrationException;
import edu.eci.dosw.brigades_and_incidents.mapper.BrigadistaBusquedaMapper;
import edu.eci.dosw.brigades_and_incidents.repository.AsignacionBrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.BrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.EstadoBrigadistaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class BuscarBrigadistaService {

    private static final String ROL_BRIGADISTA = "BRIGADISTA";
    private static final String ESTADO_ACTIVO = "ACTIVO";
    private static final String ESTADO_INACTIVO = "INACTIVO";
    private static final String ESTADO_ASIGNACION_VIGENTE = "VIGENTE";

    private final BrigadistaClient brigadistaClient;
    private final BrigadistaRepository brigadistaRepository;
    private final EstadoBrigadistaRepository estadoRepository;
    private final AsignacionBrigadistaRepository asignacionRepository;
    private final BrigadistaBusquedaMapper mapper;

    public BuscarBrigadistaService(
            BrigadistaClient brigadistaClient,
            BrigadistaRepository brigadistaRepository,
            EstadoBrigadistaRepository estadoRepository,
            AsignacionBrigadistaRepository asignacionRepository,
            BrigadistaBusquedaMapper mapper
    ) {
        this.brigadistaClient = brigadistaClient;
        this.brigadistaRepository = brigadistaRepository;
        this.estadoRepository = estadoRepository;
        this.asignacionRepository = asignacionRepository;
        this.mapper = mapper;
    }

    @Transactional
    public BusquedaBrigadistasResponse buscarBrigadistas(
            String nombreBrigadista
    ) {
        List<BrigadistaExterno> brigadistasExternos;

        try {
            brigadistasExternos = brigadistaClient.listarBrigadistas();
        } catch (Ms01IntegrationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No fue posible consultar los brigadistas en MS01",
                    exception
            );
        }

        if (brigadistasExternos == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "MS01 no devolvió información de brigadistas"
            );
        }

        sincronizarBrigadistas(brigadistasExternos);

        // Sin texto se listan todos los brigadistas (la pantalla de gestión los filtra)
        String textoBusqueda = nombreBrigadista == null ? "" : normalizar(nombreBrigadista);

        List<BrigadistaExterno> encontrados =
                brigadistasExternos.stream()
                        .filter(this::esBrigadista)
                        .filter(brigadista ->
                                normalizar(brigadista.nombre())
                                        .contains(textoBusqueda)
                        )
                        .sorted(Comparator.comparing(
                                BrigadistaExterno::nombre,
                                String.CASE_INSENSITIVE_ORDER
                        ))
                        .toList();

        List<BuscarBrigadistaResponse> resultados =
                encontrados.stream()
                        .map(this::convertirBrigadista)
                        .toList();

        String mensaje = resultados.isEmpty()
                ? "No hay resultados compatibles con la búsqueda"
                : "Brigadistas encontrados";

        return new BusquedaBrigadistasResponse(
                mensaje,
                resultados
        );
    }

    private void sincronizarBrigadistas(
            List<BrigadistaExterno> brigadistasExternos
    ) {
        EstadoBrigadista estadoInicial =
                estadoRepository.findByNombre(ESTADO_INACTIVO)
                        .orElseThrow(() -> new IllegalStateException(
                                "No existe el estado INACTIVO"
                        ));

        for (BrigadistaExterno externo : brigadistasExternos) {

            if (!esBrigadista(externo)) {
                continue;
            }

            if (externo.id() == null || externo.id().isBlank()) {
                continue;
            }

            brigadistaRepository
                    .findByUsuarioIdExterno(externo.id())
                    .orElseGet(() ->
                            brigadistaRepository.save(
                                    new Brigadista(
                                            externo.id(),
                                            estadoInicial
                                    )
                            )
                    );
        }
    }

    private BuscarBrigadistaResponse convertirBrigadista(
            BrigadistaExterno brigadistaExterno
    ) {
        Brigadista brigadistaLocal =
                brigadistaRepository
                        .findByUsuarioIdExterno(brigadistaExterno.id())
                        .orElseThrow(() -> new IllegalStateException(
                                "El brigadista no fue registrado localmente"
                        ));

        List<AsignacionBrigadista> asignaciones =
                asignacionRepository
                        .findByBrigadistaIdAndEstado_Nombre(
                                brigadistaExterno.id(),
                                ESTADO_ASIGNACION_VIGENTE
                        );

        List<AsignacionBrigadistaResumenResponse> asignacionesResponse =
                mapper.convertirAsignaciones(asignaciones);

        return new BuscarBrigadistaResponse(
                brigadistaExterno.id(),
                brigadistaExterno.nombre(),
                brigadistaExterno.rol(),
                brigadistaLocal.getEstado().getNombre(),
                asignacionesResponse
        );
    }

    @Transactional
    public void cambiarEstado(
            String brigadistaId,
            String nuevoEstado
    ) {
        if (brigadistaId == null || brigadistaId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del brigadista es obligatorio"
            );
        }

        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar el nuevo estado"
            );
        }

        String estadoNormalizado =
                nuevoEstado.trim().toUpperCase(Locale.ROOT);

        if (!estadoNormalizado.equals(ESTADO_ACTIVO)
                && !estadoNormalizado.equals(ESTADO_INACTIVO)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El estado debe ser ACTIVO o INACTIVO"
            );
        }

        Brigadista brigadista =
                brigadistaRepository
                        .findByUsuarioIdExterno(brigadistaId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "No existe el brigadista indicado"
                        ));

        EstadoBrigadista estado =
                estadoRepository.findByNombre(estadoNormalizado)
                        .orElseThrow(() -> new IllegalStateException(
                                "No existe el estado " + estadoNormalizado
                        ));

        brigadista.setEstado(estado);
        brigadistaRepository.save(brigadista);
    }

    private boolean esBrigadista(BrigadistaExterno brigadista) {
        return brigadista != null
                && brigadista.id() != null
                && !brigadista.id().isBlank()
                && brigadista.rol() != null
                && brigadista.rol().equalsIgnoreCase(ROL_BRIGADISTA)
                && brigadista.nombre() != null
                && !brigadista.nombre().isBlank();
    }

    private String normalizar(String texto) {
        return texto.trim().toLowerCase(Locale.ROOT);
    }
}
