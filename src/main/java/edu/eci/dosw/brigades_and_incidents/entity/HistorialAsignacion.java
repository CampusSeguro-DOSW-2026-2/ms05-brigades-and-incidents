
package edu.eci.dosw.brigades_and_incidents.entity;

import edu.eci.dosw.brigades_and_incidents.enums.TipoUbicacion;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "historial_asignaciones")
public class HistorialAsignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_historial")
    private UUID idHistorial;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignacion_id", nullable = false)
    private AsignacionBrigadistas asignacion;

    @Column(name = "ubicacion_anterior_id", nullable = false)
    private UUID ubicacionAnteriorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ubicacion_anterior", nullable = false)
    private TipoUbicacion tipoUbicacionAnterior;

    @Column(name = "ubicacion_nueva_id", nullable = false)
    private UUID ubicacionNuevaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ubicacion_nueva", nullable = false)
    private TipoUbicacion tipoUbicacionNueva;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;

    public HistorialAsignacion() {
    }

    @PrePersist
    protected void antesDeGuardar() {
        if (fechaCambio == null) {
            fechaCambio = LocalDateTime.now();
        }
    }

    public UUID getIdHistorial() {
        return idHistorial;
    }

    public void setIdHistorial(UUID idHistorial) {
        this.idHistorial = idHistorial;
    }

    public AsignacionBrigadistas getAsignacion() {
        return asignacion;
    }

    public void setAsignacion(AsignacionBrigadistas asignacion) {
        this.asignacion = asignacion;
    }

    public UUID getUbicacionAnteriorId() {
        return ubicacionAnteriorId;
    }

    public void setUbicacionAnteriorId(UUID ubicacionAnteriorId) {
        this.ubicacionAnteriorId = ubicacionAnteriorId;
    }

    public TipoUbicacion getTipoUbicacionAnterior() {
        return tipoUbicacionAnterior;
    }

    public void setTipoUbicacionAnterior(
            TipoUbicacion tipoUbicacionAnterior) {
        this.tipoUbicacionAnterior = tipoUbicacionAnterior;
    }

    public UUID getUbicacionNuevaId() {
        return ubicacionNuevaId;
    }

    public void setUbicacionNuevaId(UUID ubicacionNuevaId) {
        this.ubicacionNuevaId = ubicacionNuevaId;
    }

    public TipoUbicacion getTipoUbicacionNueva() {
        return tipoUbicacionNueva;
    }

    public void setTipoUbicacionNueva(
            TipoUbicacion tipoUbicacionNueva) {
        this.tipoUbicacionNueva = tipoUbicacionNueva;
    }

    public LocalDateTime getFechaCambio() {
        return fechaCambio;
    }

    public void setFechaCambio(LocalDateTime fechaCambio) {
        this.fechaCambio = fechaCambio;
    }
}
