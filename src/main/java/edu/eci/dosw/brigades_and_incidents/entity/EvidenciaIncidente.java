
package edu.eci.dosw.brigades_and_incidents.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "evidencia_incidente")
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "La referencia al incidente es una asociación ManyToOne administrada por Hibernate."
)
public class EvidenciaIncidente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String nombreArchivo;

    @Column(nullable = false, length = 100)
    private String tipoContenido;

    @Column(nullable = false, length = 500)
    private String referenciaArchivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incidente_id", nullable = false)
    private Incidente incidente;

    @Column(nullable = false)
    private OffsetDateTime fechaCarga;

    public EvidenciaIncidente() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getTipoContenido() {
        return tipoContenido;
    }

    public void setTipoContenido(String tipoContenido) {
        this.tipoContenido = tipoContenido;
    }

    public String getReferenciaArchivo() {
        return referenciaArchivo;
    }

    public void setReferenciaArchivo(String referenciaArchivo) {
        this.referenciaArchivo = referenciaArchivo;
    }

    public Incidente getIncidente() {
        return incidente;
    }

    public void setIncidente(Incidente incidente) {
        this.incidente = incidente;
    }

    public OffsetDateTime getFechaCarga() {
        return fechaCarga;
    }

    public void setFechaCarga(OffsetDateTime fechaCarga) {
        this.fechaCarga = fechaCarga;
    }

    @PrePersist
    protected void alCrear() {
        if (fechaCarga == null) {
            fechaCarga = OffsetDateTime.now();
        }
    }
}
