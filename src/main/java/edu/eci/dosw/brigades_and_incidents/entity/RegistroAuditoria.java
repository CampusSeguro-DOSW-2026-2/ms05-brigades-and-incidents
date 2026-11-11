
package edu.eci.dosw.brigades_and_incidents.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "registro_auditoria",
        indexes = {
                @Index(name = "idx_auditoria_evento", columnList = "evento_id"),
                @Index(
                        name = "idx_auditoria_recurso",
                        columnList = "recurso_tipo, recurso_id"
                ),
                @Index(
                        name = "idx_auditoria_fecha",
                        columnList = "fecha_hora_registro"
                ),
                @Index(
                        name = "idx_auditoria_tipo",
                        columnList = "tipo_registro_id"
                )
        }
)
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "El tipo de registro es una asociación ManyToOne administrada por Hibernate."
)
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_registro_id", nullable = false)
    private TipoRegistroAuditoria tipoRegistro;

    @Column(name = "usuario_origen", nullable = false, length = 100)
    private String usuarioOrigen;

    @Column(nullable = false, length = 100)
    private String accion;

    @Column(name = "recurso_tipo", nullable = false, length = 50)
    private String recursoTipo;

    @Column(name = "recurso_id", nullable = false, length = 100)
    private String recursoId;

    @Column(name = "fecha_hora_registro", nullable = false)
    private OffsetDateTime fechaHoraRegistro;

    @Column(name = "evento_id", length = 100)
    private String eventoId;

    @Column(nullable = false, length = 100)
    private String origen;

    @Column(nullable = false, length = 50)
    private String resultado;

    @Column(length = 1000)
    private String detalle;

    public RegistroAuditoria() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TipoRegistroAuditoria getTipoRegistro() {
        return tipoRegistro;
    }

    public void setTipoRegistro(TipoRegistroAuditoria tipoRegistro) {
        this.tipoRegistro = tipoRegistro;
    }

    public String getUsuarioOrigen() {
        return usuarioOrigen;
    }

    public void setUsuarioOrigen(String usuarioOrigen) {
        this.usuarioOrigen = usuarioOrigen;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getRecursoTipo() {
        return recursoTipo;
    }

    public void setRecursoTipo(String recursoTipo) {
        this.recursoTipo = recursoTipo;
    }

    public String getRecursoId() {
        return recursoId;
    }

    public void setRecursoId(String recursoId) {
        this.recursoId = recursoId;
    }

    public OffsetDateTime getFechaHoraRegistro() {
        return fechaHoraRegistro;
    }

    public void setFechaHoraRegistro(OffsetDateTime fechaHoraRegistro) {
        this.fechaHoraRegistro = fechaHoraRegistro;
    }

    public String getEventoId() {
        return eventoId;
    }

    public void setEventoId(String eventoId) {
        this.eventoId = eventoId;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    @PrePersist
    protected void alCrear() {
        if (fechaHoraRegistro == null) {
            fechaHoraRegistro = OffsetDateTime.now();
        }
    }
}
