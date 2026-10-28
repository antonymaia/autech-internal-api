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
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orquestracao do portal publico — usa Orders API do Mercado Pago.
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
    private final FaturaService faturaService;

    @Transactional(readOnly = true)
    public ClientePortalDTO buscarDadosCliente(String cnpjCpf) {
        Cliente cliente = clienteService.findByCnpjCpf(cnpjCpf);
        List<Fatura> faturas = faturaRepository.findAllByCliente(cliente);
        return new ClientePortalDTO(
                cliente.getCnpjCpf(),
                resolverRazaoSocial(cliente),
                faturas.stream()
                        // ABERTA e a fatura do proximo mes: so aparece para o cliente depois de fechada
                        .filter(f -> f.getEstado() != EstadoFatura.ABERTA)
                        .map(this::toFaturaDTO)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Cria (ou reaproveita) uma Order PIX no Mercado Pago para a fatura.
     * Idempotente: se ja existe mpOrderId na fatura e ela ainda esta pending,
     * reaproveita a mesma order em vez de criar outra.
     */
    @Transactional
    public PagamentoPixResponse gerarPagamentoPix(Long faturaId) {
        Fatura fatura = faturaRepository.findById(faturaId)
                .orElseThrow(() -> new ObjectNotFoundException("Fatura de id " + faturaId + " nao encontrada"));

        if (fatura.getEstado() == EstadoFatura.PAGA) {
            throw new BadRequestException("Fatura ja esta paga");
        }
        if (fatura.getEstado() == EstadoFatura.ABERTA) {
            throw new BadRequestException("Fatura ainda nao esta disponivel para pagamento");
        }

        try {
            if (fatura.getMpOrderId() != null && !fatura.getMpOrderId().isBlank()) {
                JsonNode orderExistente = mercadoPagoService.buscarOrder(fatura.getMpOrderId());
                if (orderExistente != null && isOrderReaproveitavel(orderExistente.path("status").asText())) {
                    return toPagamentoPixResponse(orderExistente);
                }
            }

            Cliente cliente = fatura.getCliente();
            JsonNode novaOrder = mercadoPagoService.criarOrderPix(
                    fatura.getId(),
                    fatura.getValor(),
                    cliente.getCnpjCpf(),
                    resolverRazaoSocial(cliente),
                    cliente.getEmail());

            String orderId = novaOrder.path("id").asText(null);
            if (orderId == null) {
                throw new BadRequestException("Resposta do Mercado Pago sem id da order");
            }
            fatura.setMpOrderId(orderId);
            faturaRepository.save(fatura);

            log.info("[Portal] Order PIX criada — fatura={}, mp_order_id={}, valor={}",
                    fatura.getId(), orderId, fatura.getValor());

            return toPagamentoPixResponse(novaOrder);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Portal] Erro ao gerar PIX pra fatura {}: {}", faturaId, e.getMessage(), e);
            throw new BadRequestException("Erro ao gerar pagamento PIX. Tente novamente em instantes.");
        }
    }

    /**
     * Reprocessa manualmente uma fatura consultando a order no Mercado Pago.
     * Usado por admin quando o webhook falhou (ex.: secret rotacionada).
     * Nao faz validacao HMAC — assume que o chamador tem autoridade.
     */
    @Transactional
    public boolean reprocessarFaturaViaMp(Long faturaId) {
        Fatura fatura = faturaRepository.findById(faturaId)
                .orElseThrow(() -> new ObjectNotFoundException("Fatura " + faturaId + " nao encontrada"));
        if (fatura.getMpOrderId() == null || fatura.getMpOrderId().isBlank()) {
            throw new BadRequestException("Fatura " + faturaId + " nao tem mpOrderId — nunca foi gerado PIX");
        }
        return processarWebhook(fatura.getMpOrderId(), "order");
    }

    /**
     * Processa a notificacao do Mercado Pago. Idempotente.
     * Aceita o id como order_id OU payment_id (compat com faturas antigas).
     */
    @Transactional
    public boolean processarWebhook(String id, String topic) {
        if (id == null || id.isBlank()) {
            log.warn("[Portal/Webhook] id ausente — ignorando");
            return false;
        }

        boolean tentarComoOrder = topic == null || topic.isBlank()
                || topic.toLowerCase().contains("order");
        boolean tentarComoPayment = topic == null || topic.isBlank()
                || "payment".equalsIgnoreCase(topic);

        if (tentarComoOrder) {
            JsonNode order = mercadoPagoService.buscarOrder(id);
            if (order != null && !order.isMissingNode() && !order.path("id").isMissingNode()) {
                return processarOrder(order);
            }
        }

        if (tentarComoPayment) {
            JsonNode payment = mercadoPagoService.buscarPayment(id);
            if (payment != null && !payment.isMissingNode() && !payment.path("id").isMissingNode()) {
                return processarPaymentLegacy(payment);
            }
        }

        log.warn("[Portal/Webhook] Nao encontrou order nem payment com id {}", id);
        return false;
    }

    private boolean processarOrder(JsonNode order) {
        String orderId = order.path("id").asText();
        String status = order.path("status").asText();

        if (!"processed".equalsIgnoreCase(status)) {
            log.info("[Portal/Webhook] Order {} em status '{}' — nenhuma acao", orderId, status);
            return false;
        }

        Long faturaId = extrairFaturaId(order.path("external_reference").asText(null));
        if (faturaId == null) {
            log.warn("[Portal/Webhook] external_reference invalido na order {}: '{}'",
                    orderId, order.path("external_reference").asText(null));
            return false;
        }

        Fatura fatura = faturaRepository.findById(faturaId).orElse(null);
        if (fatura == null) {
            log.warn("[Portal/Webhook] Fatura {} nao encontrada (order {})", faturaId, orderId);
            return false;
        }
        if (fatura.getEstado() == EstadoFatura.PAGA) {
            log.info("[Portal/Webhook] Fatura {} ja esta paga — ignorando duplicata", faturaId);
            return false;
        }

        BigDecimal valor = toBigDecimal(order.path("total_amount").asText(null));
        if (valor == null) valor = fatura.getValor();

        marcarFaturaPaga(fatura, orderId, null, valor);
        log.info("[Portal/Webhook] Fatura {} marcada como PAGA via order {}", faturaId, orderId);
        return true;
    }

    /** Compat com faturas criadas antes da migracao pra Orders API. */
    private boolean processarPaymentLegacy(JsonNode payment) {
        String paymentId = payment.path("id").asText();
        String status = payment.path("status").asText();

        if (!"approved".equalsIgnoreCase(status)) {
            log.info("[Portal/Webhook] Payment legacy {} em status '{}' — nenhuma acao", paymentId, status);
            return false;
        }

        Long faturaId = extrairFaturaId(payment.path("external_reference").asText(null));
        if (faturaId == null) {
            log.warn("[Portal/Webhook] external_reference invalido no payment {}", paymentId);
            return false;
        }

        Fatura fatura = faturaRepository.findById(faturaId).orElse(null);
        if (fatura == null || fatura.getEstado() == EstadoFatura.PAGA) return false;

        BigDecimal valor = toBigDecimal(payment.path("transaction_amount").asText(null));
        if (valor == null) valor = fatura.getValor();

        marcarFaturaPaga(fatura, null, paymentId, valor);
        log.info("[Portal/Webhook] Fatura {} marcada PAGA via payment legacy {}", faturaId, paymentId);
        return true;
    }

    private void marcarFaturaPaga(Fatura fatura, String orderId, String paymentId, BigDecimal valor) {
        PagamentoPix pagamentoPix = new PagamentoPix();
        pagamentoPix.setValorPagamento(valor);
        pagamentoPix.setCreatedAt(LocalDateTime.now());
        Pagamento pagSalvo = pagamentoRepository.save(pagamentoPix);

        PagamentoFaturaId pfId = new PagamentoFaturaId(pagSalvo, fatura);
        PagamentoFatura pagamentoFatura = new PagamentoFatura(pfId);
        fatura.getPagamentos().add(pagamentoFaturaRepository.save(pagamentoFatura));

        fatura.setEstado(EstadoFatura.PAGA);
        fatura.setDataPagamento(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
        if (orderId != null) fatura.setMpOrderId(orderId);
        if (paymentId != null) fatura.setMpPaymentId(paymentId);
        faturaRepository.save(fatura);

        faturaService.desbloquearSeQuitado(fatura.getCliente());
    }

    // ---------- helpers ----------

    private boolean isOrderReaproveitavel(String status) {
        // Em Orders API, o status enquanto aguarda pagamento e "action_required"
        return "action_required".equalsIgnoreCase(status)
                || "pending".equalsIgnoreCase(status)
                || "created".equalsIgnoreCase(status);
    }

    private Long extrairFaturaId(String externalRef) {
        if (externalRef == null || externalRef.isBlank()) return null;
        String numero = externalRef.startsWith("fatura-") ? externalRef.substring(7) : externalRef;
        try {
            return Long.parseLong(numero);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseIso(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return OffsetDateTime.parse(iso).toLocalDateTime();
        } catch (Exception e1) {
            try {
                return LocalDateTime.parse(iso);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private PagamentoPixResponse toPagamentoPixResponse(JsonNode order) {
        String orderId = order.path("id").asText(null);
        String status = order.path("status").asText(null);

        JsonNode payment = order.path("transactions").path("payments").path(0);
        String paymentId = payment.path("id").asText(null);

        // No Orders API, o qr_code fica DENTRO de payment_method (nao em payment_data)
        JsonNode pm = payment.path("payment_method");
        String qrCode = pm.path("qr_code").asText(null);
        String qrCodeBase64 = pm.path("qr_code_base64").asText(null);
        String ticketUrl = pm.path("ticket_url").asText(null);

        LocalDateTime expiracao = parseIso(firstNonEmpty(
                payment.path("expiration_time").asText(null),
                order.path("expiration_time").asText(null)));

        return new PagamentoPixResponse(orderId, paymentId, qrCode, qrCodeBase64, ticketUrl, expiracao, status);
    }

    private String firstNonEmpty(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
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

    private String resolverRazaoSocial(Cliente cliente) {
        if (cliente.getRazaoSocialNome() != null && !cliente.getRazaoSocialNome().isBlank()) {
            return cliente.getRazaoSocialNome();
        }
        return cliente.getNome();
    }
}
