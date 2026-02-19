package br.autech.springrestapi.dtos;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class AssinaturaProdutoDTO {

    private Long idAssinaturaProduto;

    private Long idProduto;
    private String nomeProduto;

    private Integer quantidade;
    private BigDecimal valorProduto;

    private BigDecimal desconto;
    private BigDecimal acrescimo;

    private BigDecimal valorUnitarioFinal;
    private BigDecimal valorTotal;

    private LocalDateTime dataInclusao;
}