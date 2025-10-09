package br.autech.springrestapi.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@JsonTypeName("cartaoCredito")
public class PagamentoCartaoCredito extends Pagamento {
    private String ultimosQuatroDigitos;
    private Integer numeroParcelas;
    private String bandeira;
}
