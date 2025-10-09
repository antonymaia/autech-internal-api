package br.autech.springrestapi.dtos;

import br.autech.springrestapi.model.Endereco;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
public class ClienteNovoDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "Preenchimento obrigatório")
    @Length(min=11, max=14, message="CNPJ/CPF deve ter 11 ou 14 caracteres")
    private String cnpjCpf;
    @NotEmpty(message = "Informe o nome fantasia")
    private String nome;
    @NotEmpty(message = "Informe o nome do responsável")
    private String nomeResponsavel;
    private String razaoSocialNome;
    private String telefone;
    @Email(message = "E-mail inválido")
    private String email;
    private String diaVencimento;
    private String numeroEndereco;
    private String complementoEndereco;
    private Endereco endereco;
    private BigDecimal valorMensalidade;

    public ClienteNovoDTO(){}
}
