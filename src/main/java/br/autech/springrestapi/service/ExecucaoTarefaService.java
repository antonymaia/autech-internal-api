package br.autech.springrestapi.service;

import br.autech.springrestapi.dtos.OcorrenciaExecucaoDTO;
import br.autech.springrestapi.model.ExecucaoTarefa;
import br.autech.springrestapi.model.OcorrenciaExecucao;
import br.autech.springrestapi.model.enums.StatusExecucaoTarefa;
import br.autech.springrestapi.model.enums.TarefaAgendada;
import br.autech.springrestapi.model.enums.TipoOcorrenciaExecucao;
import br.autech.springrestapi.repository.ClienteRepository;
import br.autech.springrestapi.repository.ExecucaoTarefaRepository;
import br.autech.springrestapi.repository.OcorrenciaExecucaoRepository;
import br.autech.springrestapi.service.exception.ObjectNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registro das execuções das tarefas agendadas, consultado pelo site. Os métodos de gravação nunca lançam exceção:
 * uma falha aqui só vai para o log e não pode interromper a tarefa. Cada gravação tem a sua própria transação, então
 * devem ser chamados fora da transação do que está sendo registrado, senão um rollback apaga o registro junto.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecucaoTarefaService {

    public static final String MOTIVO_ERRO_PROCESSAMENTO = "ERRO_PROCESSAMENTO";
    public static final String MOTIVO_FALHA_GERAL = "FALHA_GERAL";

    private static final int TAMANHO_MENSAGEM = 500;
    private static final int TAMANHO_MAXIMO_PAGINA = 100;
    private static final int DIAS_RETENCAO = 90;
    private static final ZoneId BRASIL = ZoneId.of("America/Sao_Paulo");

    private final ExecucaoTarefaRepository execucaoRepository;
    private final OcorrenciaExecucaoRepository ocorrenciaRepository;
    private final ClienteRepository clienteRepository;

    /** Devolve null se não conseguir gravar; os demais métodos de gravação aceitam esse null e não fazem nada. */
    public ExecucaoTarefa iniciar(TarefaAgendada tarefa) {
        try {
            ExecucaoTarefa execucao = new ExecucaoTarefa();
            execucao.setTarefa(tarefa);
            execucao.setStatus(StatusExecucaoTarefa.EM_ANDAMENTO);
            execucao.setInicio(agora());
            return execucaoRepository.save(execucao);
        } catch (Exception e) {
            log.error("[Execucao tarefa] Erro ao registrar o inicio de {}: {}", tarefa, e.getMessage(), e);
            return null;
        }
    }

    public void registrarOcorrencia(ExecucaoTarefa execucao, TipoOcorrenciaExecucao tipo, String cnpjCpf,
                                    String motivo, String mensagem) {
        if (execucao == null) return;
        try {
            OcorrenciaExecucao ocorrencia = new OcorrenciaExecucao();
            ocorrencia.setExecucao(execucao);
            ocorrencia.setTipo(tipo);
            ocorrencia.setCnpjCpf(cnpjCpf);
            ocorrencia.setMotivo(motivo);
            ocorrencia.setMensagem(limitar(mensagem));
            ocorrencia.setDataHora(agora());
            ocorrenciaRepository.save(ocorrencia);
        } catch (Exception e) {
            log.error("[Execucao tarefa] Erro ao registrar ocorrencia da execucao {} (cliente {}): {}",
                    execucao.getId(), cnpjCpf, e.getMessage(), e);
        }
    }

    public void registrarErro(ExecucaoTarefa execucao, String cnpjCpf, Exception erro) {
        registrarOcorrencia(execucao, TipoOcorrenciaExecucao.ERRO, cnpjCpf, MOTIVO_ERRO_PROCESSAMENTO, resumir(erro));
    }

    public void finalizar(ExecucaoTarefa execucao, int fechadas, int proximasGeradas, int ignorados, int erros) {
        if (execucao == null) return;
        execucao.setFechadas(fechadas);
        execucao.setProximasGeradas(proximasGeradas);
        execucao.setIgnorados(ignorados);
        execucao.setErros(erros);
        encerrar(execucao, erros > 0 ? StatusExecucaoTarefa.COM_ERROS : StatusExecucaoTarefa.SUCESSO);
    }

    /** A tarefa parou antes de terminar, por um erro que não é de um cliente específico. */
    public void falhar(ExecucaoTarefa execucao, Exception erro) {
        if (execucao == null) return;
        registrarOcorrencia(execucao, TipoOcorrenciaExecucao.ERRO, null, MOTIVO_FALHA_GERAL, resumir(erro));
        execucao.setErros(1);
        encerrar(execucao, StatusExecucaoTarefa.FALHA);
    }

    private void encerrar(ExecucaoTarefa execucao, StatusExecucaoTarefa status) {
        try {
            execucao.setStatus(status);
            execucao.setFim(agora());
            execucaoRepository.save(execucao);
        } catch (Exception e) {
            log.error("[Execucao tarefa] Erro ao registrar o fim da execucao {}: {}",
                    execucao.getId(), e.getMessage(), e);
        }
    }

    public Page<ExecucaoTarefa> buscar(TarefaAgendada tarefa, StatusExecucaoTarefa status, LocalDate de, LocalDate ate,
                                       int page, int size) {
        return execucaoRepository.buscar(tarefa, status,
                de != null ? de.atStartOfDay() : null,
                ate != null ? ate.plusDays(1).atStartOfDay() : null,
                paginacao(page, size));
    }

    public Page<OcorrenciaExecucaoDTO> buscarOcorrencias(Long execucaoId, TipoOcorrenciaExecucao tipo, int page, int size) {
        if (!execucaoRepository.existsById(execucaoId)) {
            throw new ObjectNotFoundException("Execução de id " + execucaoId + " não encontrada");
        }
        Page<OcorrenciaExecucao> ocorrencias = ocorrenciaRepository.buscar(execucaoId, tipo, paginacao(page, size));
        Map<String, String> nomes = nomesDosClientes(ocorrencias);
        return ocorrencias.map(o -> new OcorrenciaExecucaoDTO(o.getId(), o.getTipo(), o.getCnpjCpf(),
                nomes.get(o.getCnpjCpf()), o.getMotivo(), o.getMensagem(), o.getDataHora()));
    }

    // Busca separada, e nao JOIN com CLIENTES: a collation das duas colunas pode ser diferente no banco.
    private Map<String, String> nomesDosClientes(Page<OcorrenciaExecucao> ocorrencias) {
        Set<String> cnpjs = ocorrencias.stream().map(OcorrenciaExecucao::getCnpjCpf)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String, String> nomes = new HashMap<>();
        if (!cnpjs.isEmpty()) {
            clienteRepository.buscarNomes(cnpjs).forEach(c -> nomes.put(c.getCnpjCpf(), c.getNome()));
        }
        return nomes;
    }

    /** Apaga as execuções além do período de retenção e devolve quantas foram apagadas. */
    @Transactional
    public int apagarAntigas() {
        return execucaoRepository.apagarIniciadasAntesDe(agora().minusDays(DIAS_RETENCAO));
    }

    private static Pageable paginacao(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), TAMANHO_MAXIMO_PAGINA));
    }

    private static String resumir(Exception erro) {
        return erro.getMessage() != null ? erro.getMessage() : erro.getClass().getSimpleName();
    }

    private static String limitar(String mensagem) {
        return mensagem != null && mensagem.length() > TAMANHO_MENSAGEM
                ? mensagem.substring(0, TAMANHO_MENSAGEM)
                : mensagem;
    }

    private static LocalDateTime agora() {
        return LocalDateTime.now(BRASIL);
    }
}
