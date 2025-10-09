package br.autech.springrestapi.service;

import br.autech.springrestapi.dtos.TabelaNcmSiscomexDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SiscomexService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TabelaNcmSiscomexDTO buscarNcm() {
        try {
            String API_URL = "https://portalunico.siscomex.gov.br/classif/api/publico/nomenclatura/download/json?perfil=PUBLICO";
            byte[] jsonBytes = restTemplate.getForObject(API_URL, byte[].class);

            TabelaNcmSiscomexDTO tabelaNcmSiscomexDTO = objectMapper.readValue(jsonBytes, TabelaNcmSiscomexDTO.class);

            tabelaNcmSiscomexDTO.getNomenclaturas().removeIf(n -> n.getCodigo().length() != 10);

            tabelaNcmSiscomexDTO.getNomenclaturas().forEach(n -> n.setCodigo(n.getCodigo().replace(".", "")));

            return tabelaNcmSiscomexDTO;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
