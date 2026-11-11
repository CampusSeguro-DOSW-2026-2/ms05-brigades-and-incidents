package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.CambioEstadoIncidenteResponse;
import edu.eci.dosw.brigades_and_incidents.dto.HistorialIncidenteResponse;
import edu.eci.dosw.brigades_and_incidents.service.GestionAtencionIncidenteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GestionAtencionIncidenteControllerTest {

    private GestionAtencionIncidenteService service;
    private GestionAtencionIncidenteController controller;

    @BeforeEach
    void setUp() {
        service = mock(GestionAtencionIncidenteService.class);
        controller = new GestionAtencionIncidenteController(service);
    }

    @Test
    void debeAsignarResponsable() {
        UUID incidenteId = UUID.randomUUID();

        when(service.asignarResponsable(
                "evento-1", incidenteId, null, "coordinador-1"
        )).thenReturn(null);

        ResponseEntity<CambioEstadoIncidenteResponse> response =
                controller.asignarResponsable(
                        "evento-1",
                        incidenteId,
                        null,
                        "coordinador-1"
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).asignarResponsable(
                "evento-1", incidenteId, null, "coordinador-1"
        );
    }

    @Test
    void debeIniciarAtencionDelIncidente() {
        UUID incidenteId = UUID.randomUUID();

        when(service.iniciarAtencion(
                "evento-1", incidenteId, "brigadista-1"
        )).thenReturn(null);

        ResponseEntity<CambioEstadoIncidenteResponse> response =
                controller.iniciarAtencion(
                        "evento-1",
                        incidenteId,
                        "brigadista-1"
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).iniciarAtencion(
                "evento-1", incidenteId, "brigadista-1"
        );
    }

    @Test
    void debeResolverIncidente() {
        UUID incidenteId = UUID.randomUUID();

        when(service.resolverIncidente(
                "evento-1", incidenteId, null, "brigadista-1"
        )).thenReturn(null);

        ResponseEntity<CambioEstadoIncidenteResponse> response =
                controller.resolverIncidente(
                        "evento-1",
                        incidenteId,
                        null,
                        "brigadista-1"
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).resolverIncidente(
                "evento-1", incidenteId, null, "brigadista-1"
        );
    }

    @Test
    void debeCerrarIncidente() {
        UUID incidenteId = UUID.randomUUID();

        when(service.cerrarIncidente(
                "evento-1", incidenteId, "coordinador-1"
        )).thenReturn(null);

        ResponseEntity<CambioEstadoIncidenteResponse> response =
                controller.cerrarIncidente(
                        "evento-1",
                        incidenteId,
                        "coordinador-1"
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).cerrarIncidente(
                "evento-1", incidenteId, "coordinador-1"
        );
    }

    @Test
    void debeConsultarHistorialDelIncidente() {
        UUID incidenteId = UUID.randomUUID();

        when(service.consultarHistorial(
                "evento-1", incidenteId, "auditor-1"
        )).thenReturn(List.of());

        ResponseEntity<List<HistorialIncidenteResponse>> response =
                controller.consultarHistorial(
                        "evento-1",
                        incidenteId,
                        "auditor-1"
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(service).consultarHistorial(
                "evento-1", incidenteId, "auditor-1"
        );
    }

    @Test
    void debePropagarErrorAlIniciarAtencion() {
        UUID incidenteId = UUID.randomUUID();

        when(service.iniciarAtencion(
                "evento-1", incidenteId, "brigadista-1"
        )).thenThrow(new IllegalStateException("No se puede iniciar"));

        assertThrows(
                IllegalStateException.class,
                () -> controller.iniciarAtencion(
                        "evento-1", incidenteId, "brigadista-1"
                )
        );
    }
}