package edu.eci.dosw.brigades_and_incidents.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * Edificio (o zona) que forma parte del alcance de un evento. Lo envía MS06 al cambiar el estado del evento y lo
 * usan la auto asignación y el panel de gestión de brigadistas.
 */
@Embeddable
public class ZonaEvento {

    public static final String TIPO_POR_DEFECTO = "EDIFICIO";

    @Column(name = "zona_id", nullable = false, length = 100)
    private String zonaId;

    @Column(name = "nombre", length = 200)
    private String nombre;

    @Column(name = "tipo_ubicacion", nullable = false, length = 40)
    private String tipoUbicacion;

    protected ZonaEvento() {
    }

    public ZonaEvento(String zonaId, String nombre, String tipoUbicacion) {
        this.zonaId = zonaId;
        this.nombre = nombre;
        this.tipoUbicacion = tipoUbicacion == null || tipoUbicacion.isBlank()
                ? TIPO_POR_DEFECTO
                : tipoUbicacion;
    }

    public String getZonaId() {
        return zonaId;
    }

    public String getNombre() {
        return nombre;
    }

    /** Nombre para mostrar: el que envió MS06 o, si no llegó, el identificador. */
    public String getNombreVisible() {
        return nombre == null || nombre.isBlank() ? zonaId : nombre;
    }

    public String getTipoUbicacion() {
        return tipoUbicacion;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof ZonaEvento zona)) {
            return false;
        }
        return Objects.equals(zonaId, zona.zonaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zonaId);
    }
}
