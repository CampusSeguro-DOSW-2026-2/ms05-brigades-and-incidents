package edu.eci.dosw.brigades_and_incidents.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface EvidenciaStorage {

    String guardar(MultipartFile archivo);

    void eliminar(String referenciaArchivo);

    /** Ruta del archivo almacenado, validando que esté dentro del directorio de evidencias. */
    Path resolver(String referenciaArchivo);
}