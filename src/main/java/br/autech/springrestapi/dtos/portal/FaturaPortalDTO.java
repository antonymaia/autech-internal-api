package br.autech.springrestapi.dtos.portal;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Fatura exposta no portal publico. Mesmos campos da entidade Fatura,
 * exceto cliente/pagamentos (nao expostos). Itens sao resumidos.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FaturaPortalDTO {
    private Long id;
    private String estado;
    private BigDecimal valor;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dataVencimento;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dataPagamento;

    private List<FaturaItemPortalDTO> itens;
}
