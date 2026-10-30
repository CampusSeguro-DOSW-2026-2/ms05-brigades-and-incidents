
package edu.eci.dosw.brigades_and_incidents.dto;

import java.util.List;
import java.util.UUID;

public class ConfirmarAsignacionesRequest {

    private List<AsignacionConfirmada> asignaciones;

    public ConfirmarAsignacionesRequest() {
    }

    public List<AsignacionConfirmada> getAsignaciones() {
        return asignaciones;
    }

    public void setAsignaciones(List<AsignacionConfirmada> asignaciones) {
        this.asignaciones = asignaciones;
    }

    public static class AsignacionConfirmada {

        private UUID brigadistaId;
        private UUID ubicacionId;
        private String tipoUbicacion;
        private String observaciones;

        public AsignacionConfirmada() {
        }

        public UUID getBrigadistaId() {
            return brigadistaId;
        }

        public void setBrigadistaId(UUID brigadistaId) {
            this.brigadistaId = brigadistaId;
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

        public String getObservaciones() {
            return observaciones;
        }

        public void setObservaciones(String observaciones) {
            this.observaciones = observaciones;
        }
    }
}
