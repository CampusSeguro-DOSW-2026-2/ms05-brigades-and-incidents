
package edu.eci.dosw.brigades_and_incidents.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "asignacion_brigadista")
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Las referencias son asociaciones JPA administradas por Hibernate. Se conserva la identidad de las entidades relacionadas."
)
public class AsignacionBrigadista {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idAsignacion;

    @Column(name = "brigadista_id", nullable = false)
    private String brigadistaId;

    @Column(name = "evento_id", nullable = false)
    private String eventoId;

    @Column(name = "ubicacion_id", nullable = false)
    private String ubicacionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_ubicacion_id", nullable = false)
    private TipoUbicacion tipoUbicacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private EstadoAsignacion estado;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;

    @Version
    private Long version;

    public AsignacionBrigadista() {
    }

    @PrePersist
    protected void antesDeGuardar() {
        fechaCreacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void antesDeActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }

    public UUID getIdAsignacion() {
        return idAsignacion;
    }

    public String getBrigadistaId() {
        return brigadistaId;
    }

    public void setBrigadistaId(String brigadistaId) {
        this.brigadistaId = brigadistaId;
    }

    public String getEventoId() {
        return eventoId;
    }

    public void setEventoId(String eventoId) {
        this.eventoId = eventoId;
    }

    public String getUbicacionId() {
        return ubicacionId;
    }

    public void setUbicacionId(String ubicacionId) {
        this.ubicacionId = ubicacionId;
    }

    public TipoUbicacion getTipoUbicacion() {
        return tipoUbicacion;
    }

    public void setTipoUbicacion(TipoUbicacion tipoUbicacion) {
        this.tipoUbicacion = tipoUbicacion;
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

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public Long getVersion() {
        return version;
    }
}
