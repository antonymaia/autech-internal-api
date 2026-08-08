package br.autech.springrestapi.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.time.LocalDate;

@Getter
@Setter
@Entity(name = "PAGAMENTO_BOLETO")
@JsonTypeName("boleto")
public class PagamentoBoleto extends Pagamento{
   @Column(name = "DATA_VENCIMENTO")
   private LocalDate dataVencimento;
}
