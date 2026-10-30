
package edu.eci.dosw.brigades_and_incidents.dto;

import edu.eci.dosw.brigades_and_incidents.enums.TipoUbicacion;

import java.util.UUID;

public class ModificarAsignacionRequest {

    private UUID nuevaUbicacionId;
    private TipoUbicacion nuevoTipoUbicacion;
    private String nuevasObservaciones;

    public ModificarAsignacionRequest() {
    }

    public UUID getNuevaUbicacionId() {
        return nuevaUbicacionId;
    }

    public void setNuevaUbicacionId(UUID nuevaUbicacionId) {
        this.nuevaUbicacionId = nuevaUbicacionId;
    }

    public TipoUbicacion getNuevoTipoUbicacion() {
        return nuevoTipoUbicacion;
    }

    public void setNuevoTipoUbicacion(
            TipoUbicacion nuevoTipoUbicacion) {
        this.nuevoTipoUbicacion = nuevoTipoUbicacion;
    }

    public String getNuevasObservaciones() {
        return nuevasObservaciones;
    }

    public void setNuevasObservaciones(String nuevasObservaciones) {
        this.nuevasObservaciones = nuevasObservaciones;
    }
}
