package br.autech.springrestapi.service;

import br.autech.springrestapi.dtos.portal.ClientePortalDTO;
import br.autech.springrestapi.dtos.portal.FaturaItemPortalDTO;
import br.autech.springrestapi.dtos.portal.FaturaPortalDTO;
import br.autech.springrestapi.dtos.portal.PagamentoPixResponse;
import br.autech.springrestapi.model.*;
import br.autech.springrestapi.model.enums.EstadoFatura;
import br.autech.springrestapi.repository.FaturaRepository;
import br.autech.springrestapi.repository.PagamentoFaturaRepository;
import br.autech.springrestapi.repository.PagamentoRepository;
import br.autech.springrestapi.service.exception.BadRequestException;
import br.autech.springrestapi.service.exception.ObjectNotFoundException;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orquestracao do portal publico do cliente.
 * Expoe apenas os dados autorizados (nome + razao social + faturas sem dados sensiveis).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalService {

    private final ClienteService clienteService;
    private final FaturaRepository faturaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final PagamentoFaturaRepository pagamentoFaturaRepository;
    private final MercadoPagoService mercadoPagoService;

    /**
     * "Login" do portal: dado o CPF/CNPJ, retorna nome + lista de faturas.
     * Nao retorna endereco, telefone nem email.
     */
    @Transactional(readOnly = true)
    public ClientePortalDTO buscarDadosCliente(String cnpjCpf) {
        Cliente cliente = clienteService.findByCnpjCpf(cnpjCpf);
        List<Fatura> faturas = faturaRepository.findAllByCliente(cliente);
        return new ClientePortalDTO(
                cliente.getCnpjCpf(),
                resolverRazaoSocial(cliente),
                faturas.stream().map(this::toFaturaDTO).collect(Collectors.toList())
        );
    }

    /**
     * Cria (ou reaproveita) um pagamento PIX no Mercado Pago pra fatura informada.
     * Idempotente: se ja existe um mpPaymentId na fatura e ele nao esta pago/cancelado,
     * retorna o mesmo QR em vez de criar outro.
     */
    @Transactional
    public PagamentoPixResponse gerarPagamentoPix(Long faturaId) {
        Fatura fatura = faturaRepository.findById(faturaId)
                .orElseThrow(() -> new ObjectNotFoundException("Fatura de id " + faturaId + " nao encontrada"));

        if (fatura.getEstado() == EstadoFatura.PAGA) {
            throw new BadRequestException("Fatura ja esta paga");
        }

        try {
            if (fatura.getMpPaymentId() != null && !fatura.getMpPaymentId().isBlank()) {
                Payment existente = mercadoPagoService.buscarPagamento(fatura.getMpPaymentId());
                if (existente != null && isPagamentoReaproveitavel(existente.getStatus())) {
                    return toPagamentoPixResponse(existente);
                }
            }

            Cliente cliente = fatura.getCliente();
            Payment novo = mercadoPagoService.criarPagamentoPix(
                    fatura.getId(),
                    fatura.getValor(),
                    cliente.getCnpjCpf(),
                    resolverRazaoSocial(cliente),
                    cliente.getEmail());

            fatura.setMpPaymentId(String.valueOf(novo.getId()));
            faturaRepository.save(fatura);

            log.info("[Portal] PIX criado — fatura={}, mp_payment_id={}, valor={}",
                    fatura.getId(), novo.getId(), fatura.getValor());

            return toPagamentoPixResponse(novo);
        } catch (MPApiException e) {
            String body = e.getApiResponse() != null ? e.getApiResponse().getContent() : "(sem body)";
            int status = e.getApiResponse() != null ? e.getApiResponse().getStatusCode() : 0;
            log.error("[Portal] MP recusou pagamento da fatura {} (http {}): {} | body={}",
                    faturaId, status, e.getMessage(), body, e);
            throw new BadRequestException("Erro ao gerar pagamento PIX. Tente novamente em instantes.");
        } catch (MPException e) {
            log.error("[Portal] Erro MP (geral) para fatura {}: {}", faturaId, e.getMessage(), e);
            throw new BadRequestException("Erro ao gerar pagamento PIX. Tente novamente em instantes.");
        }
    }

    /**
     * Processa a notificacao do Mercado Pago. Idempotente — pode ser chamada
     * varias vezes com o mesmo payment_id sem efeitos colaterais.
     *
     * @param paymentId   id do pagamento no MP (vindo do payload do webhook)
     * @return true se marcou a fatura como PAGA, false se nao houve mudanca
     */
    @Transactional
    public boolean processarWebhook(String paymentId) {
        if (paymentId == null || paymentId.isBlank()) {
            log.warn("[Portal/Webhook] payment_id ausente — ignorando");
            return false;
        }

        try {
            Payment pagamentoMp = mercadoPagoService.buscarPagamento(paymentId);
            if (pagamentoMp == null) {
                log.warn("[Portal/Webhook] Pagamento {} nao encontrado no MP", paymentId);
                return false;
            }

            if (!"approved".equalsIgnoreCase(pagamentoMp.getStatus())) {
                log.info("[Portal/Webhook] Pagamento {} em status '{}' — nenhuma acao",
                        paymentId, pagamentoMp.getStatus());
                return false;
            }

            String externalRef = pagamentoMp.getExternalReference();
            if (externalRef == null || externalRef.isBlank()) {
                log.warn("[Portal/Webhook] Pagamento {} sem external_reference — nao da pra identificar fatura", paymentId);
                return false;
            }

            Long faturaId;
            try {
                faturaId = Long.parseLong(externalRef);
            } catch (NumberFormatException e) {
                log.warn("[Portal/Webhook] external_reference invalido '{}' no pagamento {}", externalRef, paymentId);
                return false;
            }

            Fatura fatura = faturaRepository.findById(faturaId).orElse(null);
            if (fatura == null) {
                log.warn("[Portal/Webhook] Fatura {} nao encontrada (external_reference do pagamento {})",
                        faturaId, paymentId);
                return false;
            }

            if (fatura.getEstado() == EstadoFatura.PAGA) {
                log.info("[Portal/Webhook] Fatura {} ja esta paga — ignorando duplicata do webhook", faturaId);
                return false;
            }

            PagamentoPix pagamentoPix = new PagamentoPix();
            pagamentoPix.setValorPagamento(pagamentoMp.getTransactionAmount() != null
                    ? pagamentoMp.getTransactionAmount()
                    : fatura.getValor());
            pagamentoPix.setCreatedAt(LocalDateTime.now());
            Pagamento pagSalvo = pagamentoRepository.save(pagamentoPix);

            PagamentoFaturaId pagamentoFaturaId = new PagamentoFaturaId(pagSalvo, fatura);
            PagamentoFatura pagamentoFatura = new PagamentoFatura(pagamentoFaturaId);
            fatura.getPagamentos().add(pagamentoFaturaRepository.save(pagamentoFatura));

            fatura.setEstado(EstadoFatura.PAGA);
            fatura.setDataPagamento(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
            fatura.setMpPaymentId(String.valueOf(pagamentoMp.getId()));
            faturaRepository.save(fatura);

            log.info("[Portal/Webhook] Fatura {} marcada como PAGA — pagamento MP {}, valor {}",
                    faturaId, pagamentoMp.getId(), pagamentoMp.getTransactionAmount());
            return true;
        } catch (MPException | MPApiException e) {
            log.error("[Portal/Webhook] Erro consultando pagamento {} no MP: {}", paymentId, e.getMessage(), e);
            return false;
        }
    }

    // ---------- helpers ----------

    private boolean isPagamentoReaproveitavel(String status) {
        return "pending".equalsIgnoreCase(status) || "in_process".equalsIgnoreCase(status);
    }

    private FaturaPortalDTO toFaturaDTO(Fatura f) {
        List<FaturaItemPortalDTO> itens = f.getItens().stream()
                .map(i -> new FaturaItemPortalDTO(
                        i.getDescricao(),
                        i.getValorUnitario(),
                        i.getQuantidade(),
                        i.getSubtotal()))
                .collect(Collectors.toList());
        return new FaturaPortalDTO(
                f.getId(),
                f.getEstado() != null ? f.getEstado().name() : null,
                f.getValor(),
                f.getDataVencimento(),
                f.getDataPagamento(),
                itens);
    }

    private PagamentoPixResponse toPagamentoPixResponse(Payment p) {
        String qrCode = null;
        String qrCodeBase64 = null;
        String ticketUrl = null;
        if (p.getPointOfInteraction() != null
                && p.getPointOfInteraction().getTransactionData() != null) {
            qrCode = p.getPointOfInteraction().getTransactionData().getQrCode();
            qrCodeBase64 = p.getPointOfInteraction().getTransactionData().getQrCodeBase64();
            ticketUrl = p.getPointOfInteraction().getTransactionData().getTicketUrl();
        }
        LocalDateTime expiracao = p.getDateOfExpiration() != null
                ? p.getDateOfExpiration().toLocalDateTime()
                : null;
        return new PagamentoPixResponse(
                String.valueOf(p.getId()),
                qrCode,
                qrCodeBase64,
                ticketUrl,
                expiracao,
                p.getStatus());
    }

    private String resolverRazaoSocial(Cliente cliente) {
        if (cliente.getRazaoSocialNome() != null && !cliente.getRazaoSocialNome().isBlank()) {
            return cliente.getRazaoSocialNome();
        }
        return cliente.getNome();
    }
}
