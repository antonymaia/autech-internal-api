package br.autech.springrestapi.controller;

import br.autech.springrestapi.dtos.portal.ClientePortalDTO;
import br.autech.springrestapi.dtos.portal.PagamentoPixResponse;
import br.autech.springrestapi.service.MercadoPagoService;
import br.autech.springrestapi.service.PortalService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints publicos usados pelo portal do cliente (pagar.autech.net.br).
 *
 * Seguranca:
 *  - "Login" por CPF/CNPJ apenas, sem senha (decisao de negocio)
 *  - Rate limit por IP no login (PortalRateLimitInterceptor em /pub/cliente/**)
 *  - Nao expoe telefone/email/endereco
 *  - Pagamento so vira PAGO quando o webhook do Mercado Pago confirma
 */
@Slf4j
@RestController
@RequestMapping("/pub")
@RequiredArgsConstructor
public class PortalController {

    private final PortalService portalService;
    private final MercadoPagoService mercadoPagoService;

    @GetMapping("/cliente/{cnpjCpf}")
    public ResponseEntity<ClientePortalDTO> buscarDadosCliente(@PathVariable String cnpjCpf) {
        return ResponseEntity.ok(portalService.buscarDadosCliente(cnpjCpf));
    }

    @PostMapping("/fatura/{faturaId}/pagar-pix")
    public ResponseEntity<PagamentoPixResponse> gerarPagamentoPix(@PathVariable Long faturaId) {
        return ResponseEntity.ok(portalService.gerarPagamentoPix(faturaId));
    }

    /**
     * Webhook do Mercado Pago. Chamado pelo MP automaticamente.
     *
     * O MP suporta 2 formatos:
     *  1) query-string legado: ?topic=payment&id=123
     *  2) JSON body:  {"action":"payment.updated","data":{"id":"123"},...}
     *
     * Aceitamos ambos e sempre retornamos 200 pra MP nao re-tentar infinito.
     * Em caso de assinatura invalida, loga e ignora.
     */
    @PostMapping("/webhook/mercadopago")
    public ResponseEntity<Void> webhookMercadoPago(
            @RequestBody(required = false) JsonNode body,
            @RequestParam(name = "id", required = false) String idQuery,
            @RequestParam(name = "topic", required = false) String topicQuery,
            @RequestHeader(name = "x-signature", required = false) String xSignature,
            @RequestHeader(name = "x-request-id", required = false) String xRequestId) {

        String paymentId = extrairPaymentId(body, idQuery, topicQuery);
        if (paymentId == null) {
            log.warn("[Portal/Webhook] Payload sem payment_id — ignorando");
            return ResponseEntity.ok().build();
        }

        if (!mercadoPagoService.validarAssinaturaWebhook(xSignature, xRequestId, paymentId)) {
            log.warn("[Portal/Webhook] Assinatura invalida pro pagamento {} — ignorando (header x-signature='{}')",
                    paymentId, xSignature);
            return ResponseEntity.ok().build();
        }

        portalService.processarWebhook(paymentId);
        return ResponseEntity.ok().build();
    }

    /** Extrai o payment_id do body JSON ou dos query params. */
    private String extrairPaymentId(JsonNode body, String idQuery, String topicQuery) {
        if (body != null) {
            JsonNode data = body.path("data");
            JsonNode idNode = data.path("id");
            if (!idNode.isMissingNode() && !idNode.isNull()) {
                return idNode.asText();
            }
            JsonNode resource = body.path("resource");
            if (!resource.isMissingNode() && !resource.isNull()) {
                String r = resource.asText();
                int slash = r.lastIndexOf('/');
                if (slash >= 0 && slash < r.length() - 1) return r.substring(slash + 1);
                return r;
            }
        }
        if (idQuery != null && !idQuery.isBlank()
                && (topicQuery == null || "payment".equalsIgnoreCase(topicQuery))) {
            return idQuery;
        }
        return null;
    }
}
