package br.autech.springrestapi.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class HomePageData {
   private Integer qtdeClientesAtivos;
   private Integer qtdeClientesBloqueados;
   private BigDecimal rendaAtual;
}
