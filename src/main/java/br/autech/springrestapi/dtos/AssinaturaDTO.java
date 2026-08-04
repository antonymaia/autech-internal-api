package br.autech.springrestapi.dtos;

import br.autech.springrestapi.model.enums.StatusAssinatura;
import br.autech.springrestapi.model.enums.TipoAssinatura;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;


public class AssinaturaDTO {
    @Getter
    private LocalDate data_inicio;
    @Getter
    private LocalDate data_fim;
    @Getter
    private TipoAssinatura tipoAssinatura;
    @Getter
    private String cnpjCpfCliente;
    @Getter
    private StatusAssinatura status;
    @Getter
    private List<AssinaturaProdutoDTO> produtos;
}



