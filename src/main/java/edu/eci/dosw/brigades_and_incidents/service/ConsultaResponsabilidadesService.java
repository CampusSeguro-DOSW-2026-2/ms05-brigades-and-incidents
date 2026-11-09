
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.dto.AsignacionBrigadistaResumenResponse;
import edu.eci.dosw.brigades_and_incidents.dto.AsignacionResponse;
import edu.eci.dosw.brigades_and_incidents.dto.BuscarBrigadistaResponse;
import edu.eci.dosw.brigades_and_incidents.dto.IncidenteResumenDTO;
import edu.eci.dosw.brigades_and_incidents.dto.ListadoResponsabilidadesResponse;
import edu.eci.dosw.brigades_and_incidents.dto.MetadatosPaginacionDTO;
import edu.eci.dosw.brigades_and_incidents.dto.PanelResponsabilidadesResponse;
import edu.eci.dosw.brigades_and_incidents.entity.AsignacionBrigadista;
import edu.eci.dosw.brigades_and_incidents.entity.Incidente;
import edu.eci.dosw.brigades_and_incidents.repository.AsignacionBrigadistaRepository;
import edu.eci.dosw.brigades_and_incidents.repository.IncidenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ConsultaResponsabilidadesService {

    private static final Set<String> ESTADOS_PENDIENTES =
            Set.of("REPORTADO", "ASIGNADO");

    private static final Set<String> ESTADOS_ASIGNACION =
            Set.of("VIGENTE", "RETIRADA");

    private static final Set<String> TIPOS_UBICACION =
            Set.of("EDIFICIO", "ZONA_SEGURA", "PUNTO_ENCUENTRO");

    private final AsignacionBrigadistaRepository asignacionRepository;
    private final IncidenteRepository incidenteRepository;
    private final BuscarBrigadistaService buscarBrigadistaService;

    public ConsultaResponsabilidadesService(
            AsignacionBrigadistaRepository asignacionRepository,
            IncidenteRepository incidenteRepository,
            BuscarBrigadistaService buscarBrigadistaService
    ) {
        this.asignacionRepository = asignacionRepository;
        this.incidenteRepository = incidenteRepository;
        this.buscarBrigadistaService = buscarBrigadistaService;
    }

    @Transactional(readOnly = true)
    public PanelResponsabilidadesResponse consultarPanel(
            String brigadistaId,
            int pagina,
            int tamano
    ) {
        validarPaginacion(pagina, tamano);

        if (brigadistaId == null || brigadistaId.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador del brigadista es obligatorio."
            );
        }

        List<AsignacionBrigadista> asignaciones =
                asignacionRepository.findByBrigadistaIdAndEstado_Nombre(
                        brigadistaId,
                        "VIGENTE"
                );

        List<AsignacionBrigadistaResumenResponse> asignacionesDTO =
                asignaciones.stream()
                        .map(asignacion -> new AsignacionBrigadistaResumenResponse(
                                asignacion.getIdAsignacion(),
                                asignacion.getEventoId(),
                                asignacion.getUbicacionId(),
                                asignacion.getTipoUbicacion().getNombre(),
                                asignacion.getEstado().getNombre()
                        ))
                        .toList();

        Map<UUID, Incidente> incidentesUnicos = new LinkedHashMap<>();

        for (AsignacionBrigadista asignacion : asignaciones) {

            List<Incidente> incidentes =
                    incidenteRepository.findByEventoIdAndUbicacionId(
                            asignacion.getEventoId(),
                            asignacion.getUbicacionId()
                    );

            for (Incidente incidente : incidentes) {

                boolean mismaUbicacion =
                        incidente.getTipoUbicacion().getNombre().equals(
                                asignacion.getTipoUbicacion().getNombre()
                        );

                boolean pendiente = ESTADOS_PENDIENTES.contains(
                        incidente.getEstado().getNombre()
                );

                if (mismaUbicacion && pendiente) {
                    incidentesUnicos.put(incidente.getId(), incidente);
                }
            }
        }

        List<IncidenteResumenDTO> tareas =
                incidentesUnicos.values().stream()
                        .sorted(Comparator
                                .comparingInt(this::obtenerOrdenPrioridad)
                                .thenComparing(
                                        Incidente::getFechaReporte,
                                        Comparator.reverseOrder()
                                ))
                        .map(incidente -> new IncidenteResumenDTO(
                                incidente.getId().toString(),
                                incidente.getEventoId(),
                                incidente.getUbicacionId(),
                                incidente.getTipoUbicacion().getNombre(),
                                incidente.getPrioridad().getNombre(),
                                incidente.getEstado().getNombre(),
                                incidente.getBrigadistaResponsableId(),
                                incidente.getFechaReporte()
                        ))
                        .toList();

        int totalElementos = tareas.size();
        int totalPaginas = (int) Math.ceil(
                (double) totalElementos / tamano
        );

        int inicio = (int) Math.min((long) pagina * tamano, totalElementos);
        int fin = Math.min(inicio + tamano, totalElementos);

        List<IncidenteResumenDTO> tareasPaginadas =
                tareas.subList(inicio, fin);

        MetadatosPaginacionDTO metadatos =
                new MetadatosPaginacionDTO(
                        pagina,
                        tamano,
                        totalElementos,
                        totalPaginas
                );

        return new PanelResponsabilidadesResponse(
                asignacionesDTO,
                tareasPaginadas,
                metadatos
        );
    }

    @Transactional
    public ListadoResponsabilidadesResponse consultarListadoGeneral(
            String eventoId,
            String nombreBrigadista,
            String estado,
            String tipoUbicacion,
            String ubicacionId,
            int pagina,
            int tamano
    ) {
        validarPaginacion(pagina, tamano);

        String eventoFiltro = normalizarTexto(eventoId);
        String nombreFiltro = normalizarTexto(nombreBrigadista);
        String estadoFiltro = normalizarCatalogo(
                estado,
                ESTADOS_ASIGNACION
        );
        String tipoFiltro = normalizarCatalogo(
                tipoUbicacion,
                TIPOS_UBICACION
        );
        String ubicacionFiltro = normalizarTexto(ubicacionId);

        Set<String> brigadistasEncontrados = null;

        if (nombreFiltro != null) {
            List<BuscarBrigadistaResponse> resultados =
                    buscarBrigadistaService.buscarBrigadistas(nombreFiltro)
                            .resultados();

            brigadistasEncontrados = resultados.stream()
                    .filter(brigadista ->
                            "BRIGADISTA".equalsIgnoreCase(brigadista.rol())
                    )
                    .filter(brigadista ->
                            "ACTIVO".equalsIgnoreCase(brigadista.estado())
                    )
                    .map(BuscarBrigadistaResponse::idBrigadista)
                    .collect(java.util.stream.Collectors.toSet());
        }

        Set<String> idsBrigadistasFiltro = brigadistasEncontrados;

        List<AsignacionResponse> resultados =
                asignacionRepository.findAll()
                        .stream()
                        .filter(asignacion ->
                                eventoFiltro == null
                                        || eventoFiltro.equalsIgnoreCase(
                                                asignacion.getEventoId()
                                        )
                        )
                        .filter(asignacion ->
                                estadoFiltro == null
                                        || estadoFiltro.equals(
                                                asignacion.getEstado().getNombre()
                                        )
                        )
                        .filter(asignacion ->
                                tipoFiltro == null
                                        || tipoFiltro.equals(
                                                asignacion.getTipoUbicacion().getNombre()
                                        )
                        )
                        .filter(asignacion ->
                                ubicacionFiltro == null
                                        || ubicacionFiltro.equalsIgnoreCase(
                                                asignacion.getUbicacionId()
                                        )
                        )
                        .filter(asignacion ->
                                idsBrigadistasFiltro == null
                                        || idsBrigadistasFiltro.contains(
                                                asignacion.getBrigadistaId()
                                        )
                        )
                        .sorted(Comparator.comparing(
                                AsignacionBrigadista::getFechaCreacion,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ))
                        .map(this::convertirAsignacion)
                        .toList();

        int totalElementos = resultados.size();
        int totalPaginas = (int) Math.ceil(
                (double) totalElementos / tamano
        );

        int inicio = (int) Math.min((long) pagina * tamano, totalElementos);
        int fin = Math.min(inicio + tamano, totalElementos);

        List<AsignacionResponse> asignacionesPaginadas =
                resultados.subList(inicio, fin);

        MetadatosPaginacionDTO metadatos =
                new MetadatosPaginacionDTO(
                        pagina,
                        tamano,
                        totalElementos,
                        totalPaginas
                );

        return new ListadoResponsabilidadesResponse(
                asignacionesPaginadas,
                metadatos
        );
    }

    private AsignacionResponse convertirAsignacion(
            AsignacionBrigadista asignacion
    ) {
        return new AsignacionResponse(
                asignacion.getIdAsignacion(),
                asignacion.getBrigadistaId(),
                asignacion.getEventoId(),
                asignacion.getUbicacionId(),
                asignacion.getTipoUbicacion().getNombre(),
                asignacion.getEstado().getNombre(),
                asignacion.getObservaciones(),
                asignacion.getFechaCreacion(),
                asignacion.getFechaActualizacion()
        );
    }

    private void validarPaginacion(int pagina, int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > 100) {
            throw new IllegalArgumentException(
                    "Los parámetros de paginación no son válidos."
            );
        }
    }

    private String normalizarTexto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }

    private String normalizarCatalogo(
            String valor,
            Set<String> valoresPermitidos
    ) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        String normalizado = valor.trim().toUpperCase(Locale.ROOT);

        return valoresPermitidos.contains(normalizado)
                ? normalizado
                : null;
    }

    private int obtenerOrdenPrioridad(Incidente incidente) {

        String prioridad = incidente.getPrioridad().getNombre();

        return switch (prioridad) {
            case "EMERGENCIA" -> 1;
            case "URGENCIA_MODERADA" -> 2;
            case "POCA_URGENCIA" -> 3;
            default -> 4;
        };
    }
}
