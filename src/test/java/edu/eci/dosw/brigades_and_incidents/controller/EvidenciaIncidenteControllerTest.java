package edu.eci.dosw.brigades_and_incidents.controller;

import edu.eci.dosw.brigades_and_incidents.dto.EvidenciaIncidenteResponse;
import edu.eci.dosw.brigades_and_incidents.service.EvidenciaIncidenteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EvidenciaIncidenteControllerTest {

    private EvidenciaIncidenteService service;
    private EvidenciaIncidenteController controller;

    @BeforeEach
    void setUp() {
        service = mock(EvidenciaIncidenteService.class);
        controller = new EvidenciaIncidenteController(service);
    }

    @Test
    void debeCargarEvidenciaYRetornarCreated() {
        UUID incidenteId = UUID.randomUUID();
        MultipartFile archivo = mock(MultipartFile.class);

        EvidenciaIncidenteResponse esperada = null;

        when(service.cargarEvidencia(
                "evento-1", incidenteId, archivo, "usuario-1"
        )).thenReturn(esperada);

        ResponseEntity<EvidenciaIncidenteResponse> response =
                controller.cargarEvidencia(
                        "evento-1",
                        incidenteId,
                        archivo,
                        "usuario-1"
                );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNull(response.getBody());

        verify(service).cargarEvidencia(
                "evento-1", incidenteId, archivo, "usuario-1"
        );
    }

    @Test
    void debePropagarErrorCuandoFallaLaCargaDeEvidencia() {
        UUID incidenteId = UUID.randomUUID();
        MultipartFile archivo = mock(MultipartFile.class);

        when(service.cargarEvidencia(
                "evento-1", incidenteId, archivo, "usuario-1"
        )).thenThrow(new IllegalArgumentException("Archivo inválido"));

        assertThrows(
                IllegalArgumentException.class,
                () -> controller.cargarEvidencia(
                        "evento-1",
                        incidenteId,
                        archivo,
                        "usuario-1"
                )
        );

        verify(service).cargarEvidencia(
                "evento-1", incidenteId, archivo, "usuario-1"
        );
    }

    @Test
    void debeListarEvidenciasYRetornarOk() {
        UUID incidenteId = UUID.randomUUID();

        when(service.listarEvidencias("evento-1", incidenteId))
                .thenReturn(List.of());

        ResponseEntity<List<EvidenciaIncidenteResponse>> response =
                controller.listarEvidencias("evento-1", incidenteId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(service).listarEvidencias("evento-1", incidenteId);
    }

    @Test
    void debeRetornarLasEvidenciasEncontradas() {
        UUID incidenteId = UUID.randomUUID();

        EvidenciaIncidenteResponse evidencia =
                mock(EvidenciaIncidenteResponse.class);

        when(service.listarEvidencias("evento-1", incidenteId))
                .thenReturn(List.of(evidencia));

        ResponseEntity<List<EvidenciaIncidenteResponse>> response =
                controller.listarEvidencias("evento-1", incidenteId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertSame(evidencia, response.getBody().get(0));
    }
}