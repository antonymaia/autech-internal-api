package br.autech.springrestapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Integra com o Mercado Pago via <b>Orders API (Checkout Transparente)</b>.
 * Nao usa o SDK Java oficial porque, ate a versao 2.1.17, ele so suporta a
 * Payments API (legacy). Chamadas HTTP sao feitas via RestTemplate direto.
 */
@Slf4j
@Service
public class MercadoPagoService {

    private static final String BASE_URL = "https://api.mercadopago.com";
    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final String EMAIL_FALLBACK = "pagamentos@autech.net.br";

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${mercadopago.webhook-secret:}")
    private String webhookSecret;

    @Value("${mercadopago.webhook-url:}")
    private String webhookUrl;

    /**
     * Email de "test user" do Mercado Pago. Quando preenchido, substitui o email
     * real do pagador — usado pra testar em sandbox com credencial de producao.
     * Em producao real, deixe vazio.
     */
    @Value("${mercadopago.sandbox-payer-email:}")
    private String sandboxPayerEmail;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        if (!isConfigurado()) {
            log.warn("[MercadoPago] access-token nao configurado. Pagamentos PIX nao vao funcionar ate configurar.");
            return;
        }
        log.info("[MercadoPago] Orders API em {} — webhook: {}", BASE_URL, webhookUrl);
    }

    public boolean isConfigurado() {
        return accessToken != null && !accessToken.isBlank();
    }

    /**
     * Cria uma Order PIX no Mercado Pago. Retorna o JSON cru da order,
     * de onde o chamador extrai orderId, qr_code, qr_code_base64 etc.
     *
     * @param faturaId      id da fatura (vai como external_reference)
     * @param valor         valor da fatura
     * @param cnpjCpf       documento do pagador (so digitos)
     * @param nomeCliente   nome do pagador
     * @param emailCliente  email do pagador (fallback se invalido)
     */
    public JsonNode criarOrderPix(Long faturaId, BigDecimal valor,
                                  String cnpjCpf, String nomeCliente,
                                  String emailCliente) {
        if (!isConfigurado()) {
            throw new IllegalStateException("Mercado Pago nao configurado (access-token ausente)");
        }

        boolean modoSandbox = sandboxPayerEmail != null && !sandboxPayerEmail.isBlank();
        String email = modoSandbox ? sandboxPayerEmail.trim() : validarEmailOuFallback(emailCliente);
        String valorStr = valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();

        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "online");
        body.put("total_amount", valorStr);
        body.put("external_reference", "fatura-" + faturaId);
        body.put("processing_mode", "automatic");

        ObjectNode transactions = body.putObject("transactions");
        ArrayNode payments = transactions.putArray("payments");
        ObjectNode payment = payments.addObject();
        payment.put("amount", valorStr);
        ObjectNode paymentMethod = payment.putObject("payment_method");
        paymentMethod.put("id", "pix");
        paymentMethod.put("type", "bank_transfer");

        ObjectNode payer = body.putObject("payer");
        payer.put("email", email);
        // Em sandbox (sandboxPayerEmail setado), "APRO" dispara auto-aprovacao do PIX.
        if (modoSandbox) {
            payer.put("first_name", "APRO");
        } else if (nomeCliente != null && !nomeCliente.isBlank()) {
            payer.put("first_name", nomeCliente);
        }

        HttpHeaders headers = cabecalhosAutenticados();
        headers.set("X-Idempotency-Key", UUID.randomUUID().toString());

        try {
            log.info("[MercadoPago] Enviando POST /v1/orders — fatura={}, body={}", faturaId, body);
            ResponseEntity<JsonNode> resp = restTemplate.exchange(
                    BASE_URL + "/v1/orders",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    JsonNode.class);
            log.info("[MercadoPago] Order PIX criada — fatura={}, http={}", faturaId, resp.getStatusCodeValue());
            return resp.getBody();
        } catch (RestClientResponseException e) {
            log.error("[MercadoPago] Falha ao criar Order PIX fatura {} (http {} - {}): headers={} body={}",
                    faturaId, e.getRawStatusCode(), e.getStatusText(),
                    e.getResponseHeaders(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Mercado Pago recusou (http " + e.getRawStatusCode() + "): "
                    + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[MercadoPago] Erro inesperado criando Order PIX fatura {} — tipo={}, msg={}",
                    faturaId, e.getClass().getName(), e.getMessage(), e);
            throw new RuntimeException("Erro ao chamar Mercado Pago: " + e.getClass().getSimpleName()
                    + " - " + e.getMessage());
        }
    }

    /** Consulta uma Order pelo ID. Retorna o JSON cru. */
    public JsonNode buscarOrder(String orderId) {
        if (!isConfigurado()) {
            throw new IllegalStateException("Mercado Pago nao configurado");
        }
        try {
            ResponseEntity<JsonNode> resp = restTemplate.exchange(
                    BASE_URL + "/v1/orders/" + orderId,
                    HttpMethod.GET,
                    new HttpEntity<>(cabecalhosAutenticados()),
                    JsonNode.class);
            return resp.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("[MercadoPago] Falha ao buscar order {} (http {}): {}",
                    orderId, e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            return null;
        }
    }

    /**
     * Consulta um pagamento legado (Payments API) — usado quando o webhook
     * notifica sobre payment_id vindo de orders antigas (compatibilidade).
     */
    public JsonNode buscarPayment(String paymentId) {
        if (!isConfigurado()) {
            throw new IllegalStateException("Mercado Pago nao configurado");
        }
        try {
            ResponseEntity<JsonNode> resp = restTemplate.exchange(
                    BASE_URL + "/v1/payments/" + paymentId,
                    HttpMethod.GET,
                    new HttpEntity<>(cabecalhosAutenticados()),
                    JsonNode.class);
            return resp.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("[MercadoPago] Falha ao buscar payment {} (http {}): {}",
                    paymentId, e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            return null;
        }
    }

    /**
     * Valida a assinatura HMAC-SHA256 do webhook do Mercado Pago.
     * Formato do header x-signature: "ts=1234567890,v1=abc123..."
     * Template: id:{data_id};request-id:{request_id};ts:{ts};
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

            String secret = webhookSecret.trim();
            // Mercado Pago usa o id em LOWERCASE no template HMAC — nao documentado mas confirmado empiricamente.
            String idLower = dataId.toLowerCase();
            String template = "id:" + idLower + ";"
                    + (xRequestId != null ? "request-id:" + xRequestId + ";" : "")
                    + "ts:" + ts + ";";

            String calculado = hmacHex(template, secret);
            if (calculado.equalsIgnoreCase(v1)) {
                return true;
            }

            String preview = secret.length() > 10
                    ? secret.substring(0, 4) + "..." + secret.substring(secret.length() - 4)
                    : "***";
            log.warn("[MercadoPago] HMAC nao bate — template='{}', esperado='{}', recebido='{}', secretPreview={}",
                    template, calculado, v1, preview);
            return false;
        } catch (Exception e) {
            log.error("[MercadoPago] Erro validando assinatura webhook: {}", e.getMessage(), e);
            return false;
        }
    }

    private String hmacHex(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    // ---------- helpers ----------

    private HttpHeaders cabecalhosAutenticados() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        return h;
    }

    private String validarEmailOuFallback(String email) {
        if (email == null || email.isBlank()) return EMAIL_FALLBACK;
        String trim = email.trim();
        if (!EMAIL_REGEX.matcher(trim).matches()) {
            log.warn("[MercadoPago] Email '{}' invalido no cadastro — usando fallback {}", trim, EMAIL_FALLBACK);
            return EMAIL_FALLBACK;
        }
        return trim;
    }
}
