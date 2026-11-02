package br.autech.springrestapi.dtos;

import br.autech.springrestapi.model.enums.TipoOcorrenciaExecucao;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OcorrenciaExecucaoDTO {
    private Long id;
    private TipoOcorrenciaExecucao tipo;
    private String cnpjCpf;
    private String nomeCliente;
    private String motivo;
    private String mensagem;
    private LocalDateTime dataHora;
}
