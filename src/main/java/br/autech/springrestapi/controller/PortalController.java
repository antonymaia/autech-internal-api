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
     * Formatos aceitos:
     *  1) query-string legado: ?topic=payment&id=123  OU  ?topic=merchant_order&id=...
     *  2) JSON body:  {"type":"...","action":"...","data":{"id":"..."},...}
     *
     * Aceitamos id como order_id (Orders API) OU payment_id (legacy).
     * Sempre retorna 200 pra MP nao re-tentar infinito.
     */
    @PostMapping("/webhook/mercadopago")
    public ResponseEntity<Void> webhookMercadoPago(
            @RequestBody(required = false) JsonNode body,
            @RequestParam(name = "id", required = false) String idQuery,
            @RequestParam(name = "topic", required = false) String topicQuery,
            @RequestParam(name = "type", required = false) String typeQuery,
            @RequestHeader(name = "x-signature", required = false) String xSignature,
            @RequestHeader(name = "x-request-id", required = false) String xRequestId) {

        String recursoId = extrairId(body, idQuery);
        String topic = extrairTopic(body, topicQuery, typeQuery);

        if (recursoId == null) {
            log.warn("[Portal/Webhook] Payload sem id — ignorando");
            return ResponseEntity.ok().build();
        }

        if (!mercadoPagoService.validarAssinaturaWebhook(xSignature, xRequestId, recursoId)) {
            log.warn("[Portal/Webhook] Assinatura invalida pro id {} — ignorando (header x-signature='{}')",
                    recursoId, xSignature);
            return ResponseEntity.ok().build();
        }

        portalService.processarWebhook(recursoId, topic);
        return ResponseEntity.ok().build();
    }

    private String extrairId(JsonNode body, String idQuery) {
        if (body != null) {
            JsonNode idNode = body.path("data").path("id");
            if (!idNode.isMissingNode() && !idNode.isNull()) return idNode.asText();

            JsonNode resource = body.path("resource");
            if (!resource.isMissingNode() && !resource.isNull()) {
                String r = resource.asText();
                int slash = r.lastIndexOf('/');
                return slash >= 0 && slash < r.length() - 1 ? r.substring(slash + 1) : r;
            }
        }
        return (idQuery != null && !idQuery.isBlank()) ? idQuery : null;
    }

    private String extrairTopic(JsonNode body, String topicQuery, String typeQuery) {
        if (body != null) {
            String t = body.path("type").asText(null);
            if (t != null && !t.isBlank()) return t;
            t = body.path("topic").asText(null);
            if (t != null && !t.isBlank()) return t;
        }
        if (typeQuery != null && !typeQuery.isBlank()) return typeQuery;
        if (topicQuery != null && !topicQuery.isBlank()) return topicQuery;
        return null;
    }
}
