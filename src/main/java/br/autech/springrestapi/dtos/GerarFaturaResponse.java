package br.autech.springrestapi.dtos;

import br.autech.springrestapi.model.Fatura;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GerarFaturaResponse {
    private boolean gerada;
    private String mensagem;
    private Fatura fatura;
}
