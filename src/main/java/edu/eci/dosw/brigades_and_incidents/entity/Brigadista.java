package edu.eci.dosw.brigades_and_incidents.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(
        name = "brigadista",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_brigadista_usuario_externo",
                        columnNames = "usuario_id_externo"
                )
        }
)
public class Brigadista {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id_externo", nullable = false, unique = true)
    private String usuarioIdExterno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private EstadoBrigadista estado;

    public Brigadista() {
    }

    public Brigadista(
            String usuarioIdExterno,
            EstadoBrigadista estado
    ) {
        this.usuarioIdExterno = usuarioIdExterno;
        this.estado = estado;
    }

    public UUID getId() {
        return id;
    }

    public String getUsuarioIdExterno() {
        return usuarioIdExterno;
    }

    public void setUsuarioIdExterno(String usuarioIdExterno) {
        this.usuarioIdExterno = usuarioIdExterno;
    }

    public EstadoBrigadista getEstado() {
        return estado;
    }

    public void setEstado(EstadoBrigadista estado) {
        this.estado = estado;
    }
}