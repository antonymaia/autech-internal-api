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
 * Clientes para a cobranca em lote pelo WhatsApp (GET /whatsapp/cobranca): quem recebe a mensagem e quem ficou
 * de fora, com o motivo.
 */
@Data
@NoArgsConstructor
public class CobrancaLoteDTO {

    /** Vencimento procurado (amanha ou hoje); na cobranca de vencidas, o dia de hoje. */
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dataReferencia;
    private List<Cliente> clientes = new ArrayList<>();
    /** Tem fatura na data, mas o envio cobraria outra mais antiga (com a mensagem de atraso). */
    private List<ClienteForaDaCobranca> comFaturaAnterior = new ArrayList<>();
    private List<ClienteForaDaCobranca> semTelefone = new ArrayList<>();

    public CobrancaLoteDTO(LocalDate dataReferencia) {
        this.dataReferencia = dataReferencia;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cliente {
        private String cnpjCpf;
        private String nome;
        private String telefone;
        /** Fatura que o envio vai cobrar: a pendente mais antiga do cliente. */
        private Long faturaId;
        @JsonFormat(pattern = "dd/MM/yyyy")
        private LocalDate vencimento;
        private BigDecimal valor;
        private long diasAtraso;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteForaDaCobranca {
        private String cnpjCpf;
        private String nome;
        private String motivo;
    }
}
