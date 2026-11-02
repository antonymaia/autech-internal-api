package br.autech.springrestapi.service;

import br.autech.springrestapi.model.*;
import br.autech.springrestapi.model.enums.EstadoFatura;
import br.autech.springrestapi.model.enums.StatusAssinatura;
import br.autech.springrestapi.model.enums.TarefaAgendada;
import br.autech.springrestapi.model.enums.TipoOcorrenciaExecucao;
import br.autech.springrestapi.repository.FaturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FaturaServiceFechamentoTest {

    @Mock
    FaturaRepository faturaRepository;
    @Mock
    ClienteService clienteService;
    @Mock
    PlatformTransactionManager transactionManager;
    @Mock
    ExecucaoTarefaService execucaoTarefaService;
    @InjectMocks
    FaturaService faturaService;

    private final ExecucaoTarefa execucao = new ExecucaoTarefa();

    @BeforeEach
    void setUp() {
        when(execucaoTarefaService.iniciar(TarefaAgendada.FECHAMENTO_FATURAS)).thenReturn(execucao);
        when(faturaRepository.save(any())).then(returnsFirstArg());
    }

    private Cliente cliente(String cnpjCpf, String ativo, String diaVencimento) {
        Produto produto = new Produto();
        produto.setNome("Sistema");
        AssinaturaProduto assinaturaProduto = new AssinaturaProduto();
        assinaturaProduto.setProduto(produto);
        Assinatura assinatura = new Assinatura();
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.getProdutos().add(assinaturaProduto);

        Cliente cliente = new Cliente();
        cliente.setCnpjCpf(cnpjCpf);
        cliente.setAtivo(ativo);
        cliente.setDiaVencimento(diaVencimento);
        cliente.setAssinatura(assinatura);
        when(clienteService.findByCnpjCpf(cnpjCpf)).thenReturn(cliente);
        return cliente;
    }

    private Fatura fatura(Cliente cliente) {
        Fatura fatura = new Fatura();
        fatura.setCliente(cliente);
        fatura.setEstado(EstadoFatura.ABERTA);
        fatura.setDataVencimento(LocalDate.now().plusDays(1));
        when(faturaRepository.findAllByCliente_CnpjCpfAndEstadoAndDataVencimentoBetween(
                eq(cliente.getCnpjCpf()), anyInt(), any(), any())).thenReturn(List.of(fatura));
        return fatura;
    }

    private void comFaturasAbertasDe(Fatura... faturas) {
        when(faturaRepository.findAllByEstadoAndDataVencimentoBetween(anyInt(), any(), any())).thenReturn(List.of(faturas));
    }

    @Test
    void semProblemasFinalizaComOsTotaisESemOcorrencias() {
        comFaturasAbertasDe(fatura(cliente("1", "S", "10")));

        List<Fatura> fechadas = faturaService.fecharFaturas();

        assertEquals(1, fechadas.size());
        verify(execucaoTarefaService).finalizar(execucao, 1, 1, 0, 0);
        verify(execucaoTarefaService, never()).registrarOcorrencia(any(), any(), any(), any(), any());
        verify(execucaoTarefaService, never()).registrarErro(any(), any(), any());
    }

    @Test
    void clienteInelegivelViraOcorrenciaDeIgnorado() {
        comFaturasAbertasDe(fatura(cliente("1", "N", "10")));

        faturaService.fecharFaturas();

        verify(execucaoTarefaService).registrarOcorrencia(execucao, TipoOcorrenciaExecucao.IGNORADO, "1", "CLIENTE_INATIVO", null);
        verify(execucaoTarefaService).finalizar(execucao, 0, 0, 1, 0);
    }

    @Test
    void erroEmUmClienteRegistraDepoisDoRollbackENaoParaOsOutros() {
        Fatura comErro = fatura(cliente("1", "S", "10"));
        Fatura semErro = fatura(cliente("2", "S", "10"));
        when(faturaRepository.findAllByCliente_CnpjCpfAndEstadoAndDataVencimentoBetween(eq("2"), anyInt(), any(), any()))
                .thenReturn(List.of(semErro));
        RuntimeException erro = new RuntimeException("falhou");
        when(clienteService.findByCnpjCpf("1")).thenThrow(erro);
        comFaturasAbertasDe(comErro, semErro);

        List<Fatura> fechadas = faturaService.fecharFaturas();

        assertEquals(List.of(semErro), fechadas);
        InOrder ordem = inOrder(transactionManager, execucaoTarefaService);
        ordem.verify(transactionManager).rollback(any());
        ordem.verify(execucaoTarefaService).registrarErro(execucao, "1", erro);
        verify(execucaoTarefaService).finalizar(execucao, 1, 1, 0, 1);
    }

    @Test
    void diaDeVencimentoInvalidoContaComoErroERegistraDepoisDoCommit() {
        Fatura fatura = fatura(cliente("1", "S", "abc"));
        comFaturasAbertasDe(fatura);

        List<Fatura> fechadas = faturaService.fecharFaturas();

        assertEquals(List.of(fatura), fechadas);
        InOrder ordem = inOrder(transactionManager, execucaoTarefaService);
        ordem.verify(transactionManager).commit(any());
        ordem.verify(execucaoTarefaService).registrarOcorrencia(eq(execucao), eq(TipoOcorrenciaExecucao.ERRO), eq("1"),
                eq("DIA_VENCIMENTO_INVALIDO"), contains("diaVencimento invalido ('abc')"));
        ordem.verify(execucaoTarefaService).finalizar(execucao, 1, 0, 0, 1);
    }

    @Test
    void falhaAntesDeProcessarOsClientesRegistraERelancaAExcecao() {
        RuntimeException erro = new RuntimeException("banco fora");
        when(faturaRepository.findAllByEstadoAndDataVencimentoBetween(anyInt(), any(), any())).thenThrow(erro);

        assertThrows(RuntimeException.class, () -> faturaService.fecharFaturas());

        verify(execucaoTarefaService).falhar(execucao, erro);
        verify(execucaoTarefaService, never()).finalizar(any(), anyInt(), anyInt(), anyInt(), anyInt());
    }
}
