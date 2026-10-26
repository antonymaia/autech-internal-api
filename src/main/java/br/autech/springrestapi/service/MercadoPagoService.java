package br.autech.springrestapi.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Wrapper em volta do SDK oficial do Mercado Pago.
 * Isola o uso do SDK pra facilitar troca/mock no futuro.
 */
@Slf4j
@Service
public class MercadoPagoService {

    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final String EMAIL_FALLBACK = "pagamentos@autech.net.br";

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${mercadopago.webhook-secret:}")
    private String webhookSecret;

    @Value("${mercadopago.webhook-url:}")
    private String webhookUrl;

    @PostConstruct
    public void init() {
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("[MercadoPago] access-token nao configurado. Pagamentos PIX nao vao funcionar ate configurar.");
            return;
        }
        MercadoPagoConfig.setAccessToken(accessToken);
        log.info("[MercadoPago] SDK configurado. Webhook URL: {}", webhookUrl);
    }

    public boolean isConfigurado() {
        return accessToken != null && !accessToken.isBlank();
    }

    /**
     * Cria um pagamento PIX no Mercado Pago.
     *
     * @param faturaId         id da fatura (vai como external_reference pra rastrear no webhook)
     * @param valor            valor da fatura
     * @param cnpjCpf          CPF/CNPJ do pagador (vai pro MP, nao expoe no portal)
     * @param nomeCliente      nome do cliente (vai pro MP)
     * @param emailCliente     email do cliente (MP exige; se nao tiver, usar fallback)
     * @return Payment do SDK com os dados do PIX (qr_code, qr_code_base64, ticket_url)
     */
    public Payment criarPagamentoPix(Long faturaId, BigDecimal valor,
                                     String cnpjCpf, String nomeCliente,
                                     String emailCliente) throws MPException, MPApiException {
        if (!isConfigurado()) {
            throw new MPException("Mercado Pago nao configurado (access-token ausente)");
        }

        String email = validarEmailOuFallback(emailCliente);
        String cnpjCpfLimpo = cnpjCpf == null ? "" : cnpjCpf.replaceAll("\\D", "");
        String tipoDoc = cnpjCpfLimpo.length() == 14 ? "CNPJ" : "CPF";

        PaymentPayerRequest payer = PaymentPayerRequest.builder()
                .email(email)
                .firstName(nomeCliente != null ? nomeCliente : "Cliente")
                .identification(IdentificationRequest.builder()
                        .type(tipoDoc)
                        .number(cnpjCpfLimpo)
                        .build())
                .build();

        PaymentCreateRequest req = PaymentCreateRequest.builder()
                .transactionAmount(valor)
                .description("Fatura #" + faturaId + " - Autech")
                .paymentMethodId("pix")
                .externalReference(faturaId.toString())
                .notificationUrl(webhookUrl)
                .payer(payer)
                .build();

        PaymentClient client = new PaymentClient();
        return client.create(req);
    }

    /**
     * Retorna o email informado se tiver formato valido; caso contrario usa o fallback.
     * Mercado Pago valida o formato do email no payer e recusa pagamento se invalido.
     */
    private String validarEmailOuFallback(String email) {
        if (email == null || email.isBlank()) return EMAIL_FALLBACK;
        String trim = email.trim();
        if (!EMAIL_REGEX.matcher(trim).matches()) {
            log.warn("[MercadoPago] Email '{}' invalido no cadastro — usando fallback {}", trim, EMAIL_FALLBACK);
            return EMAIL_FALLBACK;
        }
        return trim;
    }

    /** Busca um pagamento no Mercado Pago pelo ID. */
    public Payment buscarPagamento(String paymentId) throws MPException, MPApiException {
        PaymentClient client = new PaymentClient();
        return client.get(Long.parseLong(paymentId));
    }

    /**
     * Valida a assinatura HMAC-SHA256 do webhook do Mercado Pago.
     * O header x-signature vem no formato: "ts=1234567890,v1=abc123def..."
     * Template de validacao: id:{data_id};request-id:{request_id};ts:{ts};
     *
     * @param xSignature  valor do header "x-signature"
     * @param xRequestId  valor do header "x-request-id"
     * @param dataId      ID do recurso (geralmente o payment id vindo no query string)
     * @return true se a assinatura for valida
     */
    public boolean validarAssinaturaWebhook(String xSignature, String xRequestId, String dataId) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("[MercadoPago] webhook-secret nao configurado — assinatura NAO sera validada (modo inseguro).");
            return true;
        }
        if (xSignature == null || xSignature.isBlank() || dataId == null) {
            return false;
        }

        try {
            String ts = null;
            String v1 = null;
            for (String parte : xSignature.split(",")) {
                String[] kv = parte.trim().split("=", 2);
                if (kv.length != 2) continue;
                if ("ts".equals(kv[0])) ts = kv[1];
                else if ("v1".equals(kv[0])) v1 = kv[1];
            }
            if (ts == null || v1 == null) return false;

            String template = "id:" + dataId + ";"
                    + (xRequestId != null ? "request-id:" + xRequestId + ";" : "")
                    + "ts:" + ts + ";";

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(template.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));

            return hex.toString().equalsIgnoreCase(v1);
        } catch (Exception e) {
            log.error("[MercadoPago] Erro validando assinatura webhook: {}", e.getMessage(), e);
            return false;
        }
    }
}
