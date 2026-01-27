package br.autech.springrestapi.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "PRODUTO")
public class Produto implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="ID_PRODUTO")
    private String idProduto;
    private String nome;
    private BigDecimal valor;


    public Produto() {
    }

    public Produto(String nome, BigDecimal valor) {
        super();
        this.nome = nome;
        this.valor = valor;

    }


}
