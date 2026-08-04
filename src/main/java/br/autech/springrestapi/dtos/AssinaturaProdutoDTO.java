package br.autech.springrestapi.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssinaturaProdutoDTO {
    private Long produtoId;
    private Integer quantidade;
    private BigDecimal valor;
}
