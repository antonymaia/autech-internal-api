package br.autech.springrestapi.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Liga as tarefas agendadas (PagamentoTask) somente quando tarefas.agendadas.habilitadas=true, o que so acontece
 * no perfil prod. Em dev o banco e a Evolution API sao os de producao: uma instancia local rodando os jobs
 * fecharia faturas, bloquearia clientes e mandaria cobranca pelo WhatsApp em duplicidade.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "tarefas.agendadas.habilitadas", havingValue = "true")
public class SchedulingConfig {
}
