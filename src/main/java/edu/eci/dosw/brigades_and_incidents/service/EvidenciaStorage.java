package edu.eci.dosw.brigades_and_incidents.service;

import org.springframework.web.multipart.MultipartFile;

public interface EvidenciaStorage {

    String guardar(MultipartFile archivo);

    void eliminar(String referenciaArchivo);
}