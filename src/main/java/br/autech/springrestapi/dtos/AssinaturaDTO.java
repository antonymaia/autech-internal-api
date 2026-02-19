package br.autech.springrestapi.dtos;

import br.autech.springrestapi.model.AssinaturaProduto;
import br.autech.springrestapi.model.enums.StatusAssinatura;
import br.autech.springrestapi.model.enums.TipoAssinatura;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class AssinaturaDTO {
    @Getter
    private LocalDate data_inicio;

    private LocalDate data_fim;

    private BigDecimal valor_total;

    private TipoAssinatura tipoAssinatura;

    private String cnpjCpfCliente;

    private StatusAssinatura status;

    private int quantidade_caixa;

    private List<AssinaturaProdutoDTO> assinaturaProduto;




}



