package br.autech.springrestapi.dtos.portal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Item da fatura exposto no portal — snapshot da descricao + valor + quantidade.
 * Nao expoe o Produto completo.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FaturaItemPortalDTO {
    private String descricao;
    private BigDecimal valorUnitario;
    private Integer quantidade;
    private BigDecimal subtotal;
}
