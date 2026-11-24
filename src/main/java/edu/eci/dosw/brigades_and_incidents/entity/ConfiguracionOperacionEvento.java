package edu.eci.dosw.brigades_and_incidents.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(name = "configuracion_operacion_evento")
public class ConfiguracionOperacionEvento {

    @Column(name = "nombre", length = 200)
    private String nombre;

    @Column(name = "simulacro", nullable = false)
    private boolean simulacro;

    @Column(name = "estado_evento", length = 30)
    private String estadoEvento;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "evento_zona",
            joinColumns = @JoinColumn(name = "evento_id")
    )
    private Set<ZonaEvento> zonas = new LinkedHashSet<>();

    @Id
    @Column(name = "evento_id", nullable = false, length = 100)
    private String eventoId;

    @Column(name = "autoasignacion_habilitada", nullable = false)
    private boolean autoasignacionHabilitada;

    @Column(name = "panel_habilitado", nullable = false)
    private boolean panelHabilitado;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    protected ConfiguracionOperacionEvento() {
    }

    public ConfiguracionOperacionEvento(String eventoId) {
        this.eventoId = eventoId;
        this.autoasignacionHabilitada = false;
        this.panelHabilitado = false;
        this.actualizadoEn = OffsetDateTime.now();
    }

    public void habilitarAutoasignacion() {
        this.autoasignacionHabilitada = true;
        this.panelHabilitado = false;
        actualizarFecha();
    }

    public void habilitarPanel() {
        this.autoasignacionHabilitada = false;
        this.panelHabilitado = true;
        actualizarFecha();
    }

    public void deshabilitarOperacion() {
        this.autoasignacionHabilitada = false;
        this.panelHabilitado = false;
        actualizarFecha();
    }

    private void actualizarFecha() {
        this.actualizadoEn = OffsetDateTime.now();
    }

    public String getEventoId() {
        return eventoId;
    }

    public boolean isAutoasignacionHabilitada() {
        return autoasignacionHabilitada;
    }

    public boolean isPanelHabilitado() {
        return panelHabilitado;
    }

    public OffsetDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    /**
     * Guarda los datos del evento que envía MS06. Los edificios solo se reemplazan si MS06 envía alguno, para no
     * perder el alcance cuando una notificación llega sin zonas.
     */
    public void registrarDatosEvento(
            String nombre,
            boolean simulacro,
            String estadoEvento,
            List<ZonaEvento> nuevasZonas
    ) {
        if (nombre != null && !nombre.isBlank()) {
            this.nombre = nombre.trim();
        }
        this.simulacro = simulacro;
        this.estadoEvento = estadoEvento;

        if (nuevasZonas != null && !nuevasZonas.isEmpty()) {
            this.zonas.clear();
            this.zonas.addAll(nuevasZonas);
        }
        actualizarFecha();
    }

    public String getNombre() {
        return nombre;
    }

    /** Nombre para mostrar: el que envió MS06 o, si no llegó, el identificador del evento. */
    public String getNombreVisible() {
        return nombre == null || nombre.isBlank() ? eventoId : nombre;
    }

    public boolean isSimulacro() {
        return simulacro;
    }

    public String getEstadoEvento() {
        return estadoEvento;
    }

    public List<ZonaEvento> getZonas() {
        return List.copyOf(new ArrayList<>(zonas));
    }

    public Optional<ZonaEvento> buscarZona(String zonaId) {
        if (zonaId == null) {
            return Optional.empty();
        }
        return zonas.stream()
                .filter(zona -> zonaId.equals(zona.getZonaId()))
                .findFirst();
    }
}