package edu.eci.dosw.brigades_and_incidents.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "configuracion_operacion_evento")
public class ConfiguracionOperacionEvento {

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
}