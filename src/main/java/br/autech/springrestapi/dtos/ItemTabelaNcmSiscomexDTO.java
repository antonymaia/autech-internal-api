package br.autech.springrestapi.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ItemTabelaNcmSiscomexDTO {
    @JsonProperty("Codigo")
    private String codigo;
    @JsonProperty("Descricao")
    private String descricao;
    @JsonProperty("Data_Inicio")
    private String dataInicio;
    @JsonProperty("Data_Fim")
    private String dataFim;
    @JsonProperty("Tipo_Ato_Ini")
    private String tipoAtoIni;
    @JsonProperty("Numero_Ato_Ini")
    private String numeroAtoIni;
    @JsonProperty("Ano_Ato_Ini")
    private String anoAtoIni;
}
