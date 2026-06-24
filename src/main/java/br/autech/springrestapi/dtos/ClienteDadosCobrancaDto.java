package br.autech.springrestapi.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ClienteDadosCobrancaDto {
   private String cnpjCpf;
   private String nomeResponsavel;
   private String razaoSocial;
   private BigDecimal valorMensalidade;
   private String diaVencimento;
   private String telefone;
}
