package br.autech.springrestapi.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@JsonTypeName("cartaoDebito")
public class PagamentoCartaoDebito extends Pagamento{
    private String ultimosQuatroDigitos;
    private String bandeira;
}
