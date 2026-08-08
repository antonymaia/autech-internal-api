package br.autech.springrestapi.service;

import br.autech.springrestapi.dtos.TipoPagamentoDTO;
import br.autech.springrestapi.model.enums.TipoPagamento;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PagamentoService {

    public List<TipoPagamentoDTO> listarTipos() {
        return Arrays.stream(TipoPagamento.values())
                .map(t -> new TipoPagamentoDTO(t.getCodigo(), t.getDescricao()))
                .collect(Collectors.toList());
    }
}
