package br.autech.springrestapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "ESTADO")
public class Estado implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;

    @Column(name = "UF", unique = true)
    private String uf;


    @JsonProperty("codigo_uf")
    @Column(name = "CODIGO_UF", unique = true)
    private String codigoUf;

    @JsonIgnore
    @OneToMany(mappedBy = "estado")
    private List<Cidade> cidades = new ArrayList<>();

    public Estado(){}

    public Estado(Long id, String nome, String UF, String codigoUF) {
        this.id = id;
        this.nome = nome;
        this.uf = UF;
        this.codigoUf = codigoUF;
    }
}
