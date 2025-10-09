package br.autech.springrestapi.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TabelaNcmSiscomexDTO {
    @JsonProperty("Data_Ultima_Atualizacao_NCM")
    private String dataUltimaAtualizacaoNCM;
    @JsonProperty("Ato")
    private String ato;
    @JsonProperty("Nomenclaturas")
    private List<ItemTabelaNcmSiscomexDTO> nomenclaturas;
}
