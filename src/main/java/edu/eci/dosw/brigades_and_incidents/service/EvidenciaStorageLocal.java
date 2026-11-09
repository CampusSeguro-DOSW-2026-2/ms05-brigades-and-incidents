package edu.eci.dosw.brigades_and_incidents.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class EvidenciaStorageLocal implements EvidenciaStorage {

    private final Path directorio;

    public EvidenciaStorageLocal(
            @Value("${evidencias.directorio:./uploads/evidencias}")
            String directorio
    ) {
        this.directorio = Paths.get(directorio)
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public String guardar(MultipartFile archivo) {

        try {
            Files.createDirectories(directorio);

            String extension = obtenerExtension(
                    archivo.getOriginalFilename()
            );

            String nombreAlmacenado = UUID.randomUUID() + extension;

            Path destino = directorio
                    .resolve(nombreAlmacenado)
                    .normalize();

            if (!destino.startsWith(directorio)) {
                throw new IllegalArgumentException(
                        "La ruta del archivo no es válida."
                );
            }

            archivo.transferTo(destino);

            return destino.toString();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "No fue posible almacenar la evidencia.",
                    e
            );
        }
    }

    @Override
    public void eliminar(String referenciaArchivo) {

        if (referenciaArchivo == null || referenciaArchivo.isBlank()) {
            return;
        }

        try {
            Path archivo = Paths.get(referenciaArchivo)
                    .toAbsolutePath()
                    .normalize();

            if (archivo.startsWith(directorio)) {
                Files.deleteIfExists(archivo);
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "No fue posible eliminar la evidencia almacenada.",
                    e
            );
        }
    }

    private String obtenerExtension(String nombreArchivo) {

        if (nombreArchivo == null) {
            return "";
        }

        int posicion = nombreArchivo.lastIndexOf('.');

        if (posicion < 0) {
            return "";
        }

        return nombreArchivo.substring(posicion).toLowerCase();
    }
}