package br.autech.springrestapi.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

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

    @OneToOne(mappedBy = "cliente")
    @JsonBackReference
    private Assinatura assinatura;
    public Cliente() {}
    public Cliente(String cnpjCpf, String nome, String razaoSocialNome, String nomeResponsavel, Endereco endereco, BigDecimal valorMensalidade, String complementoEndereco, String numeroEndereco, String bloqueado, String diaVencimento, String ativo, String email, String telefone) {
        this.cnpjCpf = cnpjCpf;
        this.nome = nome;
        this.razaoSocialNome = razaoSocialNome;
        this.nomeResponsavel = nomeResponsavel;
        this.endereco = endereco;
        this.valorMensalidade = valorMensalidade;
        this.complementoEndereco = complementoEndereco;
        this.numeroEndereco = numeroEndereco;
        this.bloqueado = bloqueado;
        this.diaVencimento = diaVencimento;
        this.ativo = ativo;
        this.email = email;
        this.telefone = telefone;
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


    @Override
    public String toString() {
        return "{" +
                "cnpjCpf=" + cnpjCpf +
                ", nome=" + nome +
                ", telefone=" + telefone +
                ", email=" + email +
                 ", ativo=" + ativo +
                 ", diaVencimento=" + diaVencimento +
                ", bloqueado=" + bloqueado +
                ", numeroEndereco= " + numeroEndereco +
                ", complementoEndereco" + complementoEndereco;


        }
}
