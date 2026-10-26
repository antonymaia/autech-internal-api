package br.autech.springrestapi.dtos.portal;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Dados retornados pelo criar-pagamento-pix pro front renderizar o QR Code.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagamentoPixResponse {
    /** ID do pagamento no Mercado Pago (string numerica) */
    private String paymentId;

    /** Codigo PIX "copia e cola" (BR Code EMV) */
    private String qrCode;

    /** Imagem PNG do QR Code em base64 (data: ou cru) */
    private String qrCodeBase64;

    /** Link de fallback pro checkout do MP */
    private String ticketUrl;

    /** Data/hora de expiracao do QR Code (geralmente 30 min no PIX) */
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime expiracao;

    /** "pending", "approved", "cancelled", "rejected" */
    private String status;
}
