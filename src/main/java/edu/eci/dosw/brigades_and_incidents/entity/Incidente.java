package edu.eci.dosw.brigades_and_incidents.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "incidente")
public class Incidente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String eventoId;

    @Column(nullable = false, length = 150)
    private String nombreAfectado;

    @Column(nullable = false, length = 100)
    private String reportanteId;

    @Column(nullable = false, length = 100)
    private String ubicacionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_ubicacion_id", nullable = false)
    private TipoUbicacion tipoUbicacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prioridad_id", nullable = false)
    private PrioridadIncidente prioridad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_id", nullable = false)
    private EstadoIncidente estado;

    @Column(length = 1000)
    private String descripcion;

    @Column(nullable = false)
    private OffsetDateTime fechaReporte;

    public Incidente() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEventoId() {
        return eventoId;
    }

    public void setEventoId(String eventoId) {
        this.eventoId = eventoId;
    }

    public String getNombreAfectado() {
        return nombreAfectado;
    }

    public void setNombreAfectado(String nombreAfectado) {
        this.nombreAfectado = nombreAfectado;
    }

    public String getReportanteId() {
        return reportanteId;
    }

    public void setReportanteId(String reportanteId) {
        this.reportanteId = reportanteId;
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

    public PrioridadIncidente getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(PrioridadIncidente prioridad) {
        this.prioridad = prioridad;
    }

    public EstadoIncidente getEstado() {
        return estado;
    }

    public void setEstado(EstadoIncidente estado) {
        this.estado = estado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public OffsetDateTime getFechaReporte() {
        return fechaReporte;
    }

    public void setFechaReporte(OffsetDateTime fechaReporte) {
        this.fechaReporte = fechaReporte;
    }

    @PrePersist
    protected void alCrear() {
        if (fechaReporte == null) {
            fechaReporte = OffsetDateTime.now();
        }
    }
}