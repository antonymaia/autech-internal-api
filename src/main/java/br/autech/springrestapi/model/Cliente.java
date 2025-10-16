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
@Table(name = "CLIENTES")
public class Cliente implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "CNPJ_CPF")
    private String cnpjCpf;
    private String nome;
    @Column(name = "RZ_SOCIAL_NOME")
    private String razaoSocialNome;
    @Column(name = "NOME_RESPONSAVEL")
    private String nomeResponsavel;
    private String telefone;
    private String email;
    private String ativo;
    @Column(name = "DIA_VENCTO")
    private String diaVencimento;
    private String bloqueado;
    @Column(name = "NUMERO_ENDERECO")
    private String numeroEndereco;
    @Column(name = "COMPLEMENTO_ENDERECO")
    private String complementoEndereco;
    @Column(name = "VALOR_MENSALIDADE")
    private BigDecimal valorMensalidade;

    @ManyToOne
    @JoinColumn(name = "COD_ENDERECO")
    private Endereco endereco;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name ="CLIENTE_ASSINATURA", joinColumns = @JoinColumn(name = "CNPJ_CPF"), inverseJoinColumns = @JoinColumn(name="ID_PRODUTO"))
    private Set<Produto> produtos = new HashSet<>();

    public Cliente(){}

    public Cliente(String cnpjCpf, String nome, String telefone, String email, String ativo, String diaVencimento, String bloqueado, String numeroEndereco, String complementoEndereco, Endereco endereco) {
        this.cnpjCpf = cnpjCpf;
        this.nome = nome;
        this.telefone = telefone;
        this.email = email;
        this.ativo = ativo;
        this.diaVencimento = diaVencimento;
        this.bloqueado = bloqueado;
        this.numeroEndereco = numeroEndereco;
        this.complementoEndereco = complementoEndereco;
        this.endereco = endereco;
    }

    @PrePersist
    @PreUpdate
    public void toUpperCase(){
        if(this.nome != null){
            this.nome = this.nome.toUpperCase();
        }
        if (razaoSocialNome != null){
            razaoSocialNome = razaoSocialNome.toUpperCase();
        }
        if(nomeResponsavel != null){
            nomeResponsavel = nomeResponsavel.toUpperCase();
        }
    }


}
