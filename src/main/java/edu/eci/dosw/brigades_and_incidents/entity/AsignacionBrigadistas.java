
package edu.eci.dosw.brigades_and_incidents.entity;

import edu.eci.dosw.brigades_and_incidents.enums.EstadoAsignacion;
import edu.eci.dosw.brigades_and_incidents.enums.TipoAsignacion;
import edu.eci.dosw.brigades_and_incidents.enums.TipoUbicacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "asignaciones_brigadistas")
public class AsignacionBrigadistas {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_asignacion")
    private UUID idAsignacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brigadista_id", nullable = false)
    private Brigadista brigadista;

    @Column(name = "evento_id", nullable = false)
    private UUID eventoId;

    @Column(name = "ubicacion_id", nullable = false)
    private UUID ubicacionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ubicacion", nullable = false, length = 20)
    private TipoUbicacion tipoUbicacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_asignacion", nullable = false, length = 20)
    private TipoAsignacion tipoAsignacion = TipoAsignacion.DEFAULT;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoAsignacion estado = EstadoAsignacion.VIGENTE;

    @Column(name = "observaciones")
    private String observaciones;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public AsignacionBrigadistas() {
    }

    // Se ejecuta antes de guardar una asignación nueva.
    @PrePersist
    protected void antesDeGuardar() {
        LocalDateTime ahora = LocalDateTime.now();

        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }

        fechaActualizacion = ahora;
    }

    // Se ejecuta antes de actualizar una asignación existente.
    @PreUpdate
    protected void antesDeActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }

    public UUID getIdAsignacion() {
        return idAsignacion;
    }

    public void setIdAsignacion(UUID idAsignacion) {
        this.idAsignacion = idAsignacion;
    }

    public Brigadista getBrigadista() {
        return brigadista;
    }

    public void setBrigadista(Brigadista brigadista) {
        this.brigadista = brigadista;
    }

    public UUID getEventoId() {
        return eventoId;
    }

    public void setEventoId(UUID eventoId) {
        this.eventoId = eventoId;
    }

    public UUID getUbicacionId() {
        return ubicacionId;
    }

    public void setUbicacionId(UUID ubicacionId) {
        this.ubicacionId = ubicacionId;
    }

    public TipoUbicacion getTipoUbicacion() {
        return tipoUbicacion;
    }

    public void setTipoUbicacion(TipoUbicacion tipoUbicacion) {
        this.tipoUbicacion = tipoUbicacion;
    }

    public TipoAsignacion getTipoAsignacion() {
        return tipoAsignacion;
    }

    public void setTipoAsignacion(TipoAsignacion tipoAsignacion) {
        this.tipoAsignacion = tipoAsignacion;
    }

    public EstadoAsignacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoAsignacion estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
