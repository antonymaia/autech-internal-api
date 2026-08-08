package br.autech.springrestapi.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "PAGAMENTO_CARTAO_DEBITO")
@JsonTypeName("cartaoDebito")
public class PagamentoCartaoDebito extends Pagamento{
    @Column(name = "ULTIMOS_QUATRO_DIGITOS")
    private String ultimosQuatroDigitos;
    private String bandeira;
}
