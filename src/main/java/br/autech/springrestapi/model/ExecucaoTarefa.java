package br.autech.springrestapi.model;

import br.autech.springrestapi.model.enums.StatusExecucaoTarefa;
import br.autech.springrestapi.model.enums.TarefaAgendada;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "EXECUCAO_TAREFA")
public class ExecucaoTarefa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "TAREFA")
    private TarefaAgendada tarefa;
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private StatusExecucaoTarefa status;
    @Column(name = "INICIO")
    private LocalDateTime inicio;
    @Column(name = "FIM")
    private LocalDateTime fim;
    @Column(name = "FECHADAS")
    private int fechadas;
    @Column(name = "PROXIMAS_GERADAS")
    private int proximasGeradas;
    @Column(name = "IGNORADOS")
    private int ignorados;
    @Column(name = "ERROS")
    private int erros;
}
