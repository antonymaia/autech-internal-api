package br.autech.springrestapi.service;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Publicado quando o Mercado Pago confirma o PIX de uma fatura.
 * Carrega so valores porque e tratado fora da transacao, sem sessao JPA.
 */
@Getter
@Builder
public class PagamentoConfirmadoEvent {
    private final Long faturaId;
    private final String cnpjCpf;
    private final String nomeCliente;
    private final String nomeResponsavel;
    private final String telefoneCliente;
    /** Situacao depois da baixa: continua bloqueado se ainda ha outra fatura em atraso. */
    private final boolean clienteBloqueado;
    private final BigDecimal valor;
    private final LocalDateTime dataPagamento;
}
