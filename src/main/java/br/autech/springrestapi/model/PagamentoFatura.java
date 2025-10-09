package br.autech.springrestapi.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Entity
public class PagamentoFatura implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonIgnore
    @EmbeddedId
    private PagamentoFaturaId id = new PagamentoFaturaId();

    @JsonIgnore
    public Fatura getFatura(){
        return id.getFatura();
    }

    public void setFatura(Fatura fatura){
        id.setFatura(fatura);
    }

    public Pagamento getPagamento(){
        return id.getPagamento();
    }

    public void setPagamento(Pagamento pagamento){
        id.setPagamento(pagamento);
    }
}
