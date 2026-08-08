package br.autech.springrestapi.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity(name = "PAGAMENTO_CARTAO_CREDITO")
@JsonTypeName("cartaoCredito")
public class PagamentoCartaoCredito extends Pagamento {
    @Column(name = "ULTIMOS_QUATRO_DIGITOS")
    private String ultimosQuatroDigitos;
    @Column(name = "NUMERO_PARCELAS")
    private Integer numeroParcelas;
    private String bandeira;
}
