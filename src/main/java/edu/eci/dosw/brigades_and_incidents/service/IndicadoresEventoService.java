package edu.eci.dosw.brigades_and_incidents.service;

import edu.eci.dosw.brigades_and_incidents.dto.IndicadoresEventoResponse;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class IndicadoresEventoService {

    public IndicadoresEventoResponse consultar(String eventoId) {

        return new IndicadoresEventoResponse(
                eventoId,
                OffsetDateTime.now(),
                0,
                0,
                0,
                List.of(),
                List.of()
        );
    }
}