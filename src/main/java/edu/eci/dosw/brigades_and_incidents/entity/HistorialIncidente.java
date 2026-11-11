
package edu.eci.dosw.brigades_and_incidents.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "historial_incidente")
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "El incidente y los estados son asociaciones JPA administradas por Hibernate. Se conserva su identidad."
)
public class HistorialIncidente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incidente_id", nullable = false)
    private Incidente incidente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_anterior_id")
    private EstadoIncidente estadoAnterior;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_nuevo_id", nullable = false)
    private EstadoIncidente estadoNuevo;

    @Column(name = "responsable_anterior_id", length = 100)
    private String responsableAnteriorId;

    @Column(name = "responsable_nuevo_id", length = 100)
    private String responsableNuevoId;

    @Column(name = "actor_id", nullable = false, length = 100)
    private String actorId;

    @Column(length = 1000)
    private String observaciones;

    @Column(name = "fecha_cambio", nullable = false)
    private OffsetDateTime fechaCambio;

    public HistorialIncidente() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Incidente getIncidente() {
        return incidente;
    }

    public void setIncidente(Incidente incidente) {
        this.incidente = incidente;
    }

    public EstadoIncidente getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(EstadoIncidente estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public EstadoIncidente getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(EstadoIncidente estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public String getResponsableAnteriorId() {
        return responsableAnteriorId;
    }

    public void setResponsableAnteriorId(String responsableAnteriorId) {
        this.responsableAnteriorId = responsableAnteriorId;
    }

    public String getResponsableNuevoId() {
        return responsableNuevoId;
    }

    public void setResponsableNuevoId(String responsableNuevoId) {
        this.responsableNuevoId = responsableNuevoId;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public OffsetDateTime getFechaCambio() {
        return fechaCambio;
    }

    public void setFechaCambio(OffsetDateTime fechaCambio) {
        this.fechaCambio = fechaCambio;
    }

    @PrePersist
    protected void alCrear() {
        if (fechaCambio == null) {
            fechaCambio = OffsetDateTime.now();
        }
    }
}
