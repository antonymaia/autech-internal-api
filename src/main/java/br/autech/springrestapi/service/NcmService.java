package br.autech.springrestapi.service;

import br.autech.springrestapi.dtos.TabelaNcmSiscomexDTO;
import br.autech.springrestapi.model.Ncm;
import br.autech.springrestapi.repository.NcmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NcmService {
    private final SiscomexService siscomexService;
    private final NcmRepository ncmRepository;

    public void atualizarNcms(){
        TabelaNcmSiscomexDTO tabelaNcmSiscomexDTO = siscomexService.buscarNcm();

        List<Ncm> ncmList = new ArrayList<>();

        if (tabelaNcmSiscomexDTO != null && tabelaNcmSiscomexDTO.getNomenclaturas() != null) {
            tabelaNcmSiscomexDTO.getNomenclaturas().forEach(n -> {
                ncmList.add(
                        Ncm.builder()
                                .codigo(n.getCodigo())
                                .descricao(n.getDescricao())
                                .dataInicio(n.getDataInicio())
                                .dataFim(n.getDataFim())
                                .build()
                );
            });

            ncmRepository.saveAll(ncmList);
        }
    }
}
