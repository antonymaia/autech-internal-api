package br.autech.springrestapi.dtos;

import br.autech.springrestapi.model.enums.StatusAssinatura;
import br.autech.springrestapi.model.enums.TipoAssinatura;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;


public class AssinaturaDTO {
    @Getter
    private LocalDate data_inicio;
    @Getter
    private LocalDate data_fim;
    @Getter
    private BigDecimal valor_total;
    @Getter
    private TipoAssinatura tipoAssinatura;
    @Getter
    private String cnpjCpfCliente;
    @Getter
    private StatusAssinatura status;




}



