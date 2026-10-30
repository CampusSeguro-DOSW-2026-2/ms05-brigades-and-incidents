
package edu.eci.dosw.brigades_and_incidents.entity;

import edu.eci.dosw.brigades_and_incidents.enums.EstadoOperativo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "brigadistas")
public class Brigadista {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_brigadista")
    private UUID idBrigadista;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_operativo", nullable = false, length = 20)
    private EstadoOperativo estadoOperativo = EstadoOperativo.ACTIVO;

    @Column(name = "observaciones")
    private String observaciones;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public Brigadista() {
    }

    public UUID getIdBrigadista() {
        return idBrigadista;
    }

    public void setIdBrigadista(UUID idBrigadista) {
        this.idBrigadista = idBrigadista;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public EstadoOperativo getEstadoOperativo() {
        return estadoOperativo;
    }

    public void setEstadoOperativo(EstadoOperativo estadoOperativo) {
        this.estadoOperativo = estadoOperativo;
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
