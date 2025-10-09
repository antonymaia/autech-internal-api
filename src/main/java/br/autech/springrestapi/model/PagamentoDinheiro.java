package br.autech.springrestapi.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@JsonTypeName("dinheiro")
public class PagamentoDinheiro extends Pagamento{
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Entity
    @PrimaryKeyJoinColumn(name = "id")
    public static class PagamentoBoleto extends Pagamento {
        @JsonFormat(pattern="dd/MM/yyyy HH:mm:ss")
        private LocalDateTime dataVencimento;

        @JsonFormat(pattern="dd/MM/yyyy HH:mm:ss")
        private LocalDateTime dataPagamento;
    }
}
