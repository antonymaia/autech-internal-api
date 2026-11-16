package br.autech.springrestapi.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente com fatura vencida e nao paga (GET /cliente/devedores), com o total devido e as faturas em atraso.
 */
@Data
@NoArgsConstructor
public class ClienteDevedorDTO {

    private String cnpjCpf;
    private String nome;
    private String telefone;
    private String ativo;
    private String bloqueado;
    private int quantidadeFaturas;
    private BigDecimal valorTotal = BigDecimal.ZERO;
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate vencimentoMaisAntigo;
    /** Dias desde o vencimento mais antigo. */
    private long diasAtraso;
    /** Faturas em atraso, da mais antiga para a mais recente. */
    private List<FaturaDevida> faturas = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaturaDevida {
        private Long id;
        @JsonFormat(pattern = "dd/MM/yyyy")
        private LocalDate vencimento;
        private BigDecimal valor;
        private long diasAtraso;
    }
}
