
package edu.eci.dosw.brigades_and_incidents.dto;

import java.util.List;
import java.util.UUID;

public class AsignacionPredeterminadaRequest {

    private UUID eventoId;
    private List<UbicacionSeleccionada> ubicaciones;

    public AsignacionPredeterminadaRequest() {
    }

    public UUID getEventoId() {
        return eventoId;
    }

    public void setEventoId(UUID eventoId) {
        this.eventoId = eventoId;
    }

    public List<UbicacionSeleccionada> getUbicaciones() {
        return ubicaciones;
    }

    public void setUbicaciones(List<UbicacionSeleccionada> ubicaciones) {
        this.ubicaciones = ubicaciones;
    }

    public static class UbicacionSeleccionada {

        private UUID ubicacionId;
        private String tipoUbicacion;

        public UbicacionSeleccionada() {
        }

        public UUID getUbicacionId() {
            return ubicacionId;
        }

        public void setUbicacionId(UUID ubicacionId) {
            this.ubicacionId = ubicacionId;
        }

        public String getTipoUbicacion() {
            return tipoUbicacion;
        }

        public void setTipoUbicacion(String tipoUbicacion) {
            this.tipoUbicacion = tipoUbicacion;
        }
    }
}
