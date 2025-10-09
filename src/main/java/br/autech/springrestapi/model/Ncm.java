package br.autech.springrestapi.model;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "NCM")
public class Ncm {

    @Id
    @Column(name = "COD_NCM")
    private String codigo;
    private String descricao;
    @Column(name = "DATA_INICIO")
    private String dataInicio;
    @Column(name = "DATA_FIM")
    private String dataFim;
}
