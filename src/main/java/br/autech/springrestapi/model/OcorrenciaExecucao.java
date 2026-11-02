package br.autech.springrestapi.model;

import br.autech.springrestapi.model.enums.TipoOcorrenciaExecucao;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "OCORRENCIA_EXECUCAO")
public class OcorrenciaExecucao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EXECUCAO_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ExecucaoTarefa execucao;
    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO")
    private TipoOcorrenciaExecucao tipo;
    /** Null quando a falha não é de um cliente específico. */
    @Column(name = "CNPJ_CPF")
    private String cnpjCpf;
    @Column(name = "MOTIVO")
    private String motivo;
    @Column(name = "MENSAGEM", length = 500)
    private String mensagem;
    @Column(name = "DATA_HORA")
    private LocalDateTime dataHora;
}
