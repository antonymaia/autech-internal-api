package br.autech.springrestapi.service;

import br.autech.springrestapi.model.Cliente;
import br.autech.springrestapi.model.Fatura;
import br.autech.springrestapi.model.Pagamento;
import br.autech.springrestapi.model.PagamentoBoleto;
import br.autech.springrestapi.model.PagamentoCartaoCredito;
import br.autech.springrestapi.model.PagamentoCartaoDebito;
import br.autech.springrestapi.model.PagamentoDinheiro;
import br.autech.springrestapi.model.PagamentoPix;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Publicado quando uma fatura e paga: pelo PIX confirmado no Mercado Pago ou por baixa manual.
 * Carrega so valores porque e tratado fora da transacao, sem sessao JPA.
 */
@Getter
@Builder
public class PagamentoConfirmadoEvent {

    public enum Origem {
        /** Webhook do Mercado Pago confirmou o PIX. */
        MERCADO_PAGO,
        /** Pagamento informado por um colaborador em POST /faturas/{id}/pagamento. */
        MANUAL
    }

    private final Long faturaId;
    private final String cnpjCpf;
    private final String nomeCliente;
    private final String nomeResponsavel;
    private final String telefoneCliente;
    /** Situacao depois da baixa: continua bloqueado se ainda ha outra fatura em atraso. */
    private final boolean clienteBloqueado;
    private final BigDecimal valor;
    private final LocalDateTime dataPagamento;
    private final Origem origem;
    /** Ex.: "PIX", "Dinheiro", "Cartão de crédito". */
    private final String formaPagamento;

    /** Monta o evento a partir da fatura ja baixada (chamar depois do desbloqueio). */
    public static PagamentoConfirmadoEvent de(Fatura fatura, Pagamento pagamento, Origem origem) {
        PagamentoConfirmadoEventBuilder evento = builder()
            .faturaId(fatura.getId())
            .valor(pagamento.getValorPagamento())
            .dataPagamento(fatura.getDataPagamento())
            .origem(origem)
            .formaPagamento(descreverForma(pagamento));

        Cliente cliente = fatura.getCliente();
        if (cliente != null) {
            evento.cnpjCpf(cliente.getCnpjCpf())
                .nomeCliente(cliente.getNome())
                .nomeResponsavel(cliente.getNomeResponsavel())
                .telefoneCliente(cliente.getTelefone())
                .clienteBloqueado("S".equalsIgnoreCase(cliente.getBloqueado()));
        }
        return evento.build();
    }

    private static String descreverForma(Pagamento pagamento) {
        if (pagamento instanceof PagamentoPix) return "PIX";
        if (pagamento instanceof PagamentoDinheiro) return "Dinheiro";
        if (pagamento instanceof PagamentoCartaoCredito) return "Cartão de crédito";
        if (pagamento instanceof PagamentoCartaoDebito) return "Cartão de débito";
        if (pagamento instanceof PagamentoBoleto) return "Boleto";
        return "Não informada";
    }
}
