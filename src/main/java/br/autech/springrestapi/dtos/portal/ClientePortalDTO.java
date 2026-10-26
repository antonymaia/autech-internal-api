package br.autech.springrestapi.dtos.portal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Dados do cliente expostos no portal publico de pagamento.
 * Nao expoe telefone, email nem endereco por decisao de privacidade.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientePortalDTO {
    private String cnpjCpf;
    private String razaoSocial;
    private List<FaturaPortalDTO> faturas;
}
