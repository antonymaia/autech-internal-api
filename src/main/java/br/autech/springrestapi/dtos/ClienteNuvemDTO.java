package br.autech.springrestapi.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cliente que usa o sistema nuvem (GET /cliente/nuvem), consumido pela API do sistema nuvem.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteNuvemDTO {
    private String cnpjCpf;
    private String nome;
}
