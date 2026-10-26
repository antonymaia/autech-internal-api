package br.autech.springrestapi.dtos.portal;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Dados retornados pelo criar-pagamento-pix pro front renderizar o QR Code.
 * Agora via Orders API — retorna orderId. paymentId segue preenchido pela
 * primeira transacao PIX da order, pra compatibilidade com fluxos antigos.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagamentoPixResponse {
    /** ID da Order no Mercado Pago (identificador principal). */
    private String orderId;

    /** ID da transacao PIX dentro da Order (opcional). */
    private String paymentId;

    /** Codigo PIX "copia e cola" (BR Code EMV). */
    private String qrCode;

    /** Imagem PNG do QR Code em base64. */
    private String qrCodeBase64;

    /** Link de fallback pro checkout do MP. */
    private String ticketUrl;

    /** Data/hora de expiracao do QR Code. */
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime expiracao;

    /** Status da order ("pending", "processed", "canceled", "expired", "refunded"). */
    private String status;
}
