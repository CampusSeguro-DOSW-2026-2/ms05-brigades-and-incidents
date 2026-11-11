
package edu.eci.dosw.brigades_and_incidents.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EvidenciaStorageLocalTest {

    private Path directorio;
    private EvidenciaStorageLocal storage;

    @BeforeEach
    void setUp() throws IOException {
        directorio = Files.createTempDirectory("evidencias-test-");
        storage = new EvidenciaStorageLocal(directorio.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (directorio != null && Files.exists(directorio)) {
            try (var archivos = Files.walk(directorio)) {
                archivos.sorted((a, b) -> b.compareTo(a))
                        .forEach(ruta -> {
                            try {
                                Files.deleteIfExists(ruta);
                            } catch (IOException e) {
                                throw new IllegalStateException(e);
                            }
                        });
            }
        }
    }

    @Test
    void debeGuardarArchivoConExtension() throws IOException {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "foto.PNG",
                "image/png",
                "contenido de prueba".getBytes()
        );

        String referencia = storage.guardar(archivo);

        assertNotNull(referencia);
        assertTrue(referencia.endsWith(".png"));
        assertTrue(Files.exists(Path.of(referencia)));
        assertEquals(
                "contenido de prueba",
                Files.readString(Path.of(referencia))
        );
    }

    @Test
    void debeGuardarArchivoSinExtension() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "evidencia",
                "application/octet-stream",
                "contenido".getBytes()
        );

        String referencia = storage.guardar(archivo);

        assertNotNull(referencia);
        assertFalse(referencia.endsWith("."));
        assertTrue(Files.exists(Path.of(referencia)));
    }

    @Test
    void debeGuardarArchivoConNombreOriginalNulo() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                null,
                "application/octet-stream",
                "contenido".getBytes()
        );

        String referencia = storage.guardar(archivo);

        assertNotNull(referencia);
        assertTrue(Files.exists(Path.of(referencia)));
    }

    @Test
    void debeGenerarNombresDiferentesParaArchivosDistintos() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "foto.jpg",
                "image/jpeg",
                "contenido".getBytes()
        );

        String primeraReferencia = storage.guardar(archivo);
        String segundaReferencia = storage.guardar(archivo);

        assertNotEquals(primeraReferencia, segundaReferencia);
        assertTrue(Files.exists(Path.of(primeraReferencia)));
        assertTrue(Files.exists(Path.of(segundaReferencia)));
    }

    @Test
    void debeCrearElDirectorioSiNoExiste() throws IOException {
        Path nuevoDirectorio = directorio.resolve("nuevo").resolve("nivel");
        EvidenciaStorageLocal otroStorage =
                new EvidenciaStorageLocal(nuevoDirectorio.toString());

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "foto.jpg",
                "image/jpeg",
                "contenido".getBytes()
        );

        String referencia = otroStorage.guardar(archivo);

        assertTrue(Files.isDirectory(nuevoDirectorio));
        assertTrue(Files.exists(Path.of(referencia)));
    }

    @Test
    void debeEliminarArchivoExistente() throws IOException {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "foto.jpg",
                "image/jpeg",
                "contenido".getBytes()
        );

        String referencia = storage.guardar(archivo);
        Path archivoGuardado = Path.of(referencia);

        assertTrue(Files.exists(archivoGuardado));

        storage.eliminar(referencia);

        assertFalse(Files.exists(archivoGuardado));
    }

    @Test
    void noDebeFallarCuandoLaReferenciaEsNula() {
        assertDoesNotThrow(() -> storage.eliminar(null));
    }

    @Test
    void noDebeFallarCuandoLaReferenciaEstaVacia() {
        assertDoesNotThrow(() -> storage.eliminar(""));
    }

    @Test
    void noDebeFallarCuandoLaReferenciaTieneSoloEspacios() {
        assertDoesNotThrow(() -> storage.eliminar("   "));
    }

    @Test
    void noDebeEliminarArchivosFueraDelDirectorioPermitido()
            throws IOException {

        Path directorioExterno =
                Files.createTempDirectory("externo-test-");

        try {
            Path archivoExterno =
                    directorioExterno.resolve("importante.txt");

            Files.writeString(archivoExterno, "no eliminar");

            storage.eliminar(archivoExterno.toString());

            assertTrue(Files.exists(archivoExterno));
        } finally {
            Files.deleteIfExists(
                    directorioExterno.resolve("importante.txt")
            );
            Files.deleteIfExists(directorioExterno);
        }
    }

    @Test
    void noDebeFallarAlEliminarUnArchivoInexistente() {
        Path inexistente = directorio.resolve("no-existe.txt");

        assertDoesNotThrow(
                () -> storage.eliminar(inexistente.toString())
        );
    }

    @Test
    void debeConvertirErrorDeTransferenciaEnIllegalStateException() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "foto.jpg",
                "image/jpeg",
                "contenido".getBytes()
        ) {
            @Override
            public void transferTo(Path destino) throws IOException {
                throw new IOException("Error simulado");
            }
        };

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> storage.guardar(archivo)
        );

        assertEquals(
                "No fue posible almacenar la evidencia.",
                exception.getMessage()
        );

        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void debeConvertirErrorDeEliminacionEnIllegalStateException()
            throws IOException {

        Path archivo = directorio.resolve("carpeta");
        Files.createDirectory(archivo);

        // Se utiliza un directorio no vacío para provocar un error
        // al intentar eliminarlo como si fuera un archivo.
        Files.writeString(archivo.resolve("contenido.txt"), "prueba");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> storage.eliminar(archivo.toString())
        );

        assertEquals(
                "No fue posible eliminar la evidencia almacenada.",
                exception.getMessage()
        );

        assertInstanceOf(IOException.class, exception.getCause());
    }
}
