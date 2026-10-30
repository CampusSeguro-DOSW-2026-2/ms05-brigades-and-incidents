
package edu.eci.dosw.brigades_and_incidents.entity;

import edu.eci.dosw.brigades_and_incidents.enums.TipoUbicacion;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(
    name = "configuracion_asignaciones",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_configuracion_tipo_ubicacion",
            columnNames = "tipo_ubicacion"
        )
    }
)
public class ConfiguracionAsignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idConfiguracion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ubicacion", nullable = false)
    private TipoUbicacion tipoUbicacion;

    @Column(name = "cantidad_brigadistas", nullable = false)
    private Integer cantidadBrigadistas;

    public ConfiguracionAsignacion() {
    }

    public ConfiguracionAsignacion(
            TipoUbicacion tipoUbicacion,
            Integer cantidadBrigadistas) {
        this.tipoUbicacion = tipoUbicacion;
        this.cantidadBrigadistas = cantidadBrigadistas;
    }

    public UUID getIdConfiguracion() {
        return idConfiguracion;
    }

    public void setIdConfiguracion(UUID idConfiguracion) {
        this.idConfiguracion = idConfiguracion;
    }

    public TipoUbicacion getTipoUbicacion() {
        return tipoUbicacion;
    }

    public void setTipoUbicacion(TipoUbicacion tipoUbicacion) {
        this.tipoUbicacion = tipoUbicacion;
    }

    public Integer getCantidadBrigadistas() {
        return cantidadBrigadistas;
    }

    public void setCantidadBrigadistas(Integer cantidadBrigadistas) {
        this.cantidadBrigadistas = cantidadBrigadistas;
    }
}
