package br.autech.springrestapi.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ClienteDTO {
   private String cnpjCpf;
   private String razaoSocial;
   private String nomeFantasia;
   private String nomeResponsavel;
   private String bairro;
   private String cidade;
   private String uf;
   private String telefone;
   private String bloqueado;
   private String ativo;
   private BigDecimal mensalidade;
   private String diaVencimento;
}
