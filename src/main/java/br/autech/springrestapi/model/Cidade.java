package br.autech.springrestapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "CIDADE")
public class Cidade implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;

    @JsonProperty("cod_municipio")
    @Column(name = "CODIGO_MUNICIPIO", unique = true)
    private String codigoMunicipio;

    @ManyToOne
    @JoinColumn(name = "ESTADO_ID")
    private Estado estado;

    public Cidade() {
    }

    public Cidade(Long id, String nome, String codigoMunicipio, Estado estado) {
        this.id = id;
        this.nome = nome;
        this.codigoMunicipio = codigoMunicipio;
        this.estado = estado;
    }
}
