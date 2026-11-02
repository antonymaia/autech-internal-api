package br.autech.springrestapi.service;

import br.autech.springrestapi.dtos.OcorrenciaExecucaoDTO;
import br.autech.springrestapi.model.Cliente;
import br.autech.springrestapi.model.ExecucaoTarefa;
import br.autech.springrestapi.model.enums.StatusExecucaoTarefa;
import br.autech.springrestapi.model.enums.TarefaAgendada;
import br.autech.springrestapi.model.enums.TipoOcorrenciaExecucao;
import br.autech.springrestapi.repository.ClienteRepository;
import br.autech.springrestapi.repository.ExecucaoTarefaRepository;
import br.autech.springrestapi.repository.OcorrenciaExecucaoRepository;
import br.autech.springrestapi.service.exception.ObjectNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Roda as consultas de verdade em um H2 em memória, sem encostar no banco remoto. */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@Import(ExecucaoTarefaService.class)
class ExecucaoTarefaServiceTest {

    @Autowired
    ExecucaoTarefaService service;
    @Autowired
    ExecucaoTarefaRepository execucaoRepository;
    @Autowired
    OcorrenciaExecucaoRepository ocorrenciaRepository;
    @Autowired
    ClienteRepository clienteRepository;
    @Autowired
    TestEntityManager em;
    // Injetados na classe App (carregada pelo @DataJpaTest)
    @MockBean
    ClienteService clienteService;
    @MockBean
    AssinaturaService assinaturaService;

    private ExecucaoTarefa execucao(StatusExecucaoTarefa status, LocalDateTime inicio) {
        ExecucaoTarefa execucao = new ExecucaoTarefa();
        execucao.setTarefa(TarefaAgendada.FECHAMENTO_FATURAS);
        execucao.setStatus(status);
        execucao.setInicio(inicio);
        return execucaoRepository.save(execucao);
    }

    @Test
    void sucessoSemErros() {
        ExecucaoTarefa execucao = service.iniciar(TarefaAgendada.FECHAMENTO_FATURAS);
        assertEquals(StatusExecucaoTarefa.EM_ANDAMENTO, execucao.getStatus());
        assertNull(execucao.getFim());

        service.finalizar(execucao, 3, 2, 1, 0);

        ExecucaoTarefa salva = execucaoRepository.findById(execucao.getId()).orElseThrow();
        assertEquals(StatusExecucaoTarefa.SUCESSO, salva.getStatus());
        assertNotNull(salva.getFim());
        assertEquals(3, salva.getFechadas());
        assertEquals(2, salva.getProximasGeradas());
        assertEquals(1, salva.getIgnorados());
        assertEquals(0, salva.getErros());
    }

    @Test
    void terminarComErroFicaComErros() {
        ExecucaoTarefa execucao = service.iniciar(TarefaAgendada.FECHAMENTO_FATURAS);
        service.finalizar(execucao, 3, 2, 0, 1);
        assertEquals(StatusExecucaoTarefa.COM_ERROS, execucao.getStatus());
    }

    @Test
    void falhaGeralRegistraOcorrenciaSemCliente() {
        ExecucaoTarefa execucao = service.iniciar(TarefaAgendada.FECHAMENTO_FATURAS);

        service.falhar(execucao, new IllegalStateException("banco fora"));

        assertEquals(StatusExecucaoTarefa.FALHA, execucao.getStatus());
        assertEquals(1, execucao.getErros());
        OcorrenciaExecucaoDTO ocorrencia = service.buscarOcorrencias(execucao.getId(), null, 0, 20).getContent().get(0);
        assertNull(ocorrencia.getCnpjCpf());
        assertNull(ocorrencia.getNomeCliente());
        assertEquals(ExecucaoTarefaService.MOTIVO_FALHA_GERAL, ocorrencia.getMotivo());
        assertEquals("banco fora", ocorrencia.getMensagem());
    }

    @Test
    void erroSemMensagemUsaONomeDaExcecaoEMensagemLongaECortada() {
        ExecucaoTarefa execucao = service.iniciar(TarefaAgendada.FECHAMENTO_FATURAS);

        service.registrarErro(execucao, "111", new NullPointerException());
        service.registrarErro(execucao, "222", new RuntimeException("x".repeat(800)));

        List<OcorrenciaExecucaoDTO> ocorrencias = service.buscarOcorrencias(execucao.getId(), null, 0, 20).getContent();
        assertEquals("NullPointerException", ocorrencias.get(0).getMensagem());
        assertEquals(500, ocorrencias.get(1).getMensagem().length());
    }

    @Test
    void ocorrenciasTrazemONomeDoClienteEFiltramPorTipo() {
        Cliente cliente = new Cliente();
        cliente.setCnpjCpf("11765624000116");
        cliente.setNome("Cliente Teste");
        clienteRepository.save(cliente);
        ExecucaoTarefa execucao = service.iniciar(TarefaAgendada.FECHAMENTO_FATURAS);
        service.registrarOcorrencia(execucao, TipoOcorrenciaExecucao.ERRO, "11765624000116", "M1", "falhou");
        service.registrarOcorrencia(execucao, TipoOcorrenciaExecucao.IGNORADO, "cliente-excluido", "M2", null);
        em.flush();

        Page<OcorrenciaExecucaoDTO> todas = service.buscarOcorrencias(execucao.getId(), null, 0, 20);
        Page<OcorrenciaExecucaoDTO> erros = service.buscarOcorrencias(execucao.getId(), TipoOcorrenciaExecucao.ERRO, 0, 20);

        assertEquals(2, todas.getTotalElements());
        assertEquals("CLIENTE TESTE", todas.getContent().get(0).getNomeCliente());
        assertNull(todas.getContent().get(1).getNomeCliente());
        assertEquals(1, erros.getTotalElements());
        assertEquals("M1", erros.getContent().get(0).getMotivo());
    }

    @Test
    void ocorrenciasDeExecucaoInexistenteDaoNaoEncontrado() {
        assertThrows(ObjectNotFoundException.class, () -> service.buscarOcorrencias(999L, null, 0, 20));
    }

    @Test
    void buscaFiltraEOrdenaDaMaisRecenteParaAMaisAntiga() {
        ExecucaoTarefa dia1 = execucao(StatusExecucaoTarefa.SUCESSO, LocalDateTime.of(2026, 11, 1, 5, 0));
        ExecucaoTarefa dia2 = execucao(StatusExecucaoTarefa.COM_ERROS, LocalDateTime.of(2026, 11, 2, 23, 59));
        ExecucaoTarefa dia3 = execucao(StatusExecucaoTarefa.SUCESSO, LocalDateTime.of(2026, 11, 3, 0, 0));
        em.flush();

        assertEquals(List.of(dia3, dia2, dia1), service.buscar(null, null, null, null, 0, 20).getContent());
        assertEquals(List.of(dia2), service.buscar(null, StatusExecucaoTarefa.COM_ERROS, null, null, 0, 20).getContent());
        assertEquals(List.of(dia3, dia1),
                service.buscar(TarefaAgendada.FECHAMENTO_FATURAS, StatusExecucaoTarefa.SUCESSO, null, null, 0, 20).getContent());
        // "de" e "ate" incluem o dia inteiro
        LocalDate dois = LocalDate.of(2026, 11, 2);
        assertEquals(List.of(dia2), service.buscar(null, null, dois, dois, 0, 20).getContent());
        assertEquals(List.of(dia3, dia2), service.buscar(null, null, dois, null, 0, 20).getContent());
        assertEquals(List.of(dia2, dia1), service.buscar(null, null, null, dois, 0, 20).getContent());
    }

    @Test
    void paginacaoInvalidaOuGrandeDemaisEAjustada() {
        Page<ExecucaoTarefa> pagina = service.buscar(null, null, null, null, -1, 5000);
        assertEquals(0, pagina.getNumber());
        assertEquals(100, pagina.getSize());
        assertEquals(1, service.buscar(null, null, null, null, 0, 0).getSize());
    }

    @Test
    void limpezaApagaSoAsAntigasEAsOcorrenciasDelas() {
        LocalDateTime agora = LocalDateTime.now();
        ExecucaoTarefa antiga = execucao(StatusExecucaoTarefa.SUCESSO, agora.minusDays(92));
        ExecucaoTarefa recente = execucao(StatusExecucaoTarefa.SUCESSO, agora.minusDays(88));
        service.registrarOcorrencia(antiga, TipoOcorrenciaExecucao.ERRO, "111", "M", null);
        service.registrarOcorrencia(recente, TipoOcorrenciaExecucao.ERRO, "222", "M", null);
        em.flush();
        em.clear();

        assertEquals(1, service.apagarAntigas());

        em.clear();
        assertEquals(List.of(recente.getId()), execucaoRepository.findAll().stream().map(ExecucaoTarefa::getId)
                .collect(java.util.stream.Collectors.toList()));
        assertEquals(1, ocorrenciaRepository.count());
        assertEquals("222", ocorrenciaRepository.findAll().get(0).getCnpjCpf());
    }

    @Test
    void falhaAoGravarNaoInterrompeATarefa() {
        ExecucaoTarefaRepository execucoes = mock(ExecucaoTarefaRepository.class);
        OcorrenciaExecucaoRepository ocorrencias = mock(OcorrenciaExecucaoRepository.class);
        when(execucoes.save(any())).thenThrow(new RuntimeException("banco fora"));
        when(ocorrencias.save(any())).thenThrow(new RuntimeException("banco fora"));
        ExecucaoTarefaService comFalha = new ExecucaoTarefaService(execucoes, ocorrencias, mock(ClienteRepository.class));

        assertNull(comFalha.iniciar(TarefaAgendada.FECHAMENTO_FATURAS));
        ExecucaoTarefa execucao = new ExecucaoTarefa();
        assertDoesNotThrow(() -> comFalha.registrarOcorrencia(execucao, TipoOcorrenciaExecucao.ERRO, "1", "M", null));
        assertDoesNotThrow(() -> comFalha.finalizar(execucao, 0, 0, 0, 0));
        assertDoesNotThrow(() -> comFalha.falhar(execucao, new RuntimeException("x")));
        // Sem registro de inicio, os demais nao fazem nada
        assertDoesNotThrow(() -> comFalha.registrarErro(null, "1", new RuntimeException("x")));
        assertDoesNotThrow(() -> comFalha.finalizar(null, 0, 0, 0, 0));
        assertDoesNotThrow(() -> comFalha.falhar(null, new RuntimeException("x")));
    }
}
