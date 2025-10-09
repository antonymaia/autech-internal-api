package br.autech.springrestapi.dtos;

import br.autech.springrestapi.model.Cliente;
import br.autech.springrestapi.model.Pagamento;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FaturaDTO {
    @NotNull(message = "Informe a data de vencimento da fatura")
    private LocalDateTime dataVencimento;
    private LocalDateTime dataPagamento;
    @NotNull(message = "Informe o valor da fatura")
    private BigDecimal valor;
    @NotNull(message = "Informe a data de criação da fatura")
    private LocalDateTime createdAt;
    @NotBlank(message = "Informe o status da fatura")
    private String status;
    @NotNull(message = "Informe o cliente da fatura")
    private Cliente cliente;
    private List<Pagamento> pagamentos = new ArrayList<>();
}
