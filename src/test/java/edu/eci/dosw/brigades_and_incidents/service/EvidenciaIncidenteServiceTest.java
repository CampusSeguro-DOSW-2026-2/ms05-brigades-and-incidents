
package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.client.UsuarioClient;
import edu.eci.dosw.brigades_and_incidents.entity.Incidente;
import edu.eci.dosw.brigades_and_incidents.entity.EvidenciaIncidente;
import edu.eci.dosw.brigades_and_incidents.event.IncidenteAuditPublisher;
import edu.eci.dosw.brigades_and_incidents.exception.EvidenciaIncidenteException;
import edu.eci.dosw.brigades_and_incidents.repository.EvidenciaIncidenteRepository;
import edu.eci.dosw.brigades_and_incidents.repository.IncidenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EvidenciaIncidenteServiceTest {

    private static final String EVENTO_ID = "EVENTO-001";
    private static final String USUARIO_ID = "USUARIO-001";

    private static final UUID INCIDENTE_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private EvidenciaIncidenteRepository evidenciaRepository;
    private IncidenteRepository incidenteRepository;
    private EvidenciaStorage evidenciaStorage;
    private IncidenteAuditPublisher auditPublisher;
    private UsuarioClient usuarioClient;

    private EvidenciaIncidenteService evidenciaService;

    private Incidente incidente;

    @BeforeEach
    void configurar() {

        evidenciaRepository = mock(EvidenciaIncidenteRepository.class);
        incidenteRepository = mock(IncidenteRepository.class);
        evidenciaStorage = mock(EvidenciaStorage.class);
        auditPublisher = mock(IncidenteAuditPublisher.class);
        usuarioClient = mock(UsuarioClient.class);

        evidenciaService = new EvidenciaIncidenteService(
                evidenciaRepository,
                incidenteRepository,
                evidenciaStorage,
                auditPublisher,
                usuarioClient
        );

        incidente = new Incidente();
        incidente.setId(INCIDENTE_ID);
        incidente.setEventoId(EVENTO_ID);

        when(usuarioClient.consultarUsuario(USUARIO_ID))
                .thenReturn(new UsuarioClient.UsuarioExterno(
                        USUARIO_ID,
                        "Usuario de prueba",
                        List.of("ESTUDIANTE"),
                        "ACTIVO"
                ));

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.of(incidente));

        when(evidenciaRepository.countByIncidenteId(INCIDENTE_ID))
                .thenReturn(0L);

        when(evidenciaStorage.guardar(any()))
                .thenReturn("./uploads/evidencias/archivo.png");

        when(evidenciaRepository.save(any(EvidenciaIncidente.class)))
                .thenAnswer(invocation -> {
                    EvidenciaIncidente evidencia =
                            invocation.getArgument(0);

                    evidencia.setId(UUID.randomUUID());
                    return evidencia;
                });
    }

    @Test
    void puedeCargarEvidenciaPng() {

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "evidencia.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        var respuesta = evidenciaService.cargarEvidencia(
                EVENTO_ID,
                INCIDENTE_ID,
                archivo,
                USUARIO_ID
        );

        assertNotNull(respuesta);
        assertEquals("evidencia.png", respuesta.nombreArchivo());
        assertEquals("image/png", respuesta.tipoContenido());

        verify(usuarioClient).consultarUsuario(USUARIO_ID);
        verify(evidenciaRepository).save(any(EvidenciaIncidente.class));

        verify(auditPublisher).publicar(
                eq("CARGAR_EVIDENCIA"),
                eq("EVIDENCIA_INCIDENTE"),
                anyString(),
                eq(EVENTO_ID),
                eq(USUARIO_ID),
                eq("EXITOSO"),
                anyString()
        );
    }

    @Test
    void rechazaArchivoVacio() {

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "vacio.png",
                "image/png",
                new byte[0]
        );

        assertThrows(
                EvidenciaIncidenteException.class,
                () -> evidenciaService.cargarEvidencia(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        archivo,
                        USUARIO_ID
                )
        );

        verify(evidenciaStorage, never()).guardar(any());
        verify(evidenciaRepository, never()).save(any());
    }

    @Test
    void rechazaFormatoNoPermitido() {

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "evidencia.txt",
                "text/plain",
                new byte[]{1, 2, 3}
        );

        assertThrows(
                EvidenciaIncidenteException.class,
                () -> evidenciaService.cargarEvidencia(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        archivo,
                        USUARIO_ID
                )
        );

        verify(evidenciaStorage, never()).guardar(any());
    }

    @Test
    void rechazaMasDeCincoEvidencias() {

        when(evidenciaRepository.countByIncidenteId(INCIDENTE_ID))
                .thenReturn(5L);

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "evidencia.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        assertThrows(
                EvidenciaIncidenteException.class,
                () -> evidenciaService.cargarEvidencia(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        archivo,
                        USUARIO_ID
                )
        );

        verify(evidenciaStorage, never()).guardar(any());
        verify(evidenciaRepository, never()).save(any());
    }

    @Test
    void rechazaIncidenteDeOtroEvento() {

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "evidencia.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        assertThrows(
                EvidenciaIncidenteException.class,
                () -> evidenciaService.cargarEvidencia(
                        "EVENTO-999",
                        INCIDENTE_ID,
                        archivo,
                        USUARIO_ID
                )
        );

        verify(evidenciaStorage, never()).guardar(any());
        verify(evidenciaRepository, never()).save(any());
    }

    @Test
    void rechazaIncidenteInexistente() {

        when(incidenteRepository.findById(INCIDENTE_ID))
                .thenReturn(Optional.empty());

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "evidencia.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        assertThrows(
                EvidenciaIncidenteException.class,
                () -> evidenciaService.cargarEvidencia(
                        EVENTO_ID,
                        INCIDENTE_ID,
                        archivo,
                        USUARIO_ID
                )
        );

        verify(evidenciaStorage, never()).guardar(any());
    }
}
