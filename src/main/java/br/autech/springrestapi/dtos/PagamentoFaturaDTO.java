package br.autech.springrestapi.dtos;

import br.autech.springrestapi.model.Fatura;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PagamentoFaturaDTO {
    private BigDecimal valorPagamento;
    private Integer formaPagamento;
    private Integer status;
    private Fatura fatura;
}
