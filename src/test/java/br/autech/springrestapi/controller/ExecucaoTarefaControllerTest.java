package br.autech.springrestapi.controller;

import br.autech.springrestapi.config.PortalRateLimitInterceptor;
import br.autech.springrestapi.config.SecurityConfig;
import br.autech.springrestapi.model.Usuario;
import br.autech.springrestapi.model.enums.StatusExecucaoTarefa;
import br.autech.springrestapi.model.enums.TipoOcorrenciaExecucao;
import br.autech.springrestapi.repository.AssinaturaRepository;
import br.autech.springrestapi.service.AssinaturaService;
import br.autech.springrestapi.service.ClienteService;
import br.autech.springrestapi.service.ExecucaoTarefaService;
import br.autech.springrestapi.service.TokenService;
import br.autech.springrestapi.service.exception.ObjectNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExecucaoTarefaController.class)
@Import({SecurityConfig.class, TokenService.class})
@TestPropertySource(properties = "jwt.secret=c2VncmVkby1kZS10ZXN0ZS1jb20tbWFpcy1kZS0zMi1ieXRlcy0xMjM0NTY=")
class ExecucaoTarefaControllerTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    TokenService tokenService;
    @MockBean
    ExecucaoTarefaService execucaoTarefaService;
    @MockBean
    PortalRateLimitInterceptor portalRateLimitInterceptor;
    // Injetados na classe App (carregada pelo @WebMvcTest)
    @MockBean
    ClienteService clienteService;
    @MockBean
    AssinaturaRepository assinaturaRepository;
    @MockBean
    AssinaturaService assinaturaService;

    private String token(boolean podePublicar, boolean podeVerExecucoes) {
        Usuario usuario = new Usuario(1L, "fulano", null, null, null);
        usuario.setPodePublicar(podePublicar);
        usuario.setPodeVerExecucoes(podeVerExecucoes);
        return "Bearer " + tokenService.gerar(usuario);
    }

    @Test
    void semTokenDa401NasDuasRotas() throws Exception {
        mvc.perform(get("/execucoes-tarefa")).andExpect(status().isUnauthorized());
        mvc.perform(get("/execucoes-tarefa/1/ocorrencias")).andExpect(status().isUnauthorized());
        verifyNoInteractions(execucaoTarefaService);
    }

    @Test
    void tokenInvalidoDa401() throws Exception {
        mvc.perform(get("/execucoes-tarefa").header("Authorization", "Bearer token-do-portal-do-cliente"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void permissaoDePublicarNaoBastaDa403() throws Exception {
        mvc.perform(get("/execucoes-tarefa").header("Authorization", token(true, false)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(execucaoTarefaService);
    }

    @Test
    void comPermissaoRepassaOsFiltros() throws Exception {
        when(execucaoTarefaService.buscar(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(Page.empty());
        mvc.perform(get("/execucoes-tarefa")
                        .param("status", "COM_ERROS").param("de", "2026-11-01").param("ate", "2026-11-02")
                        .header("Authorization", token(false, true)))
                .andExpect(status().isOk());
        verify(execucaoTarefaService).buscar(null, StatusExecucaoTarefa.COM_ERROS,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), 0, 20);
    }

    @Test
    void filtroInvalidoDa400() throws Exception {
        mvc.perform(get("/execucoes-tarefa").param("status", "NAO_EXISTE").header("Authorization", token(false, true)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(execucaoTarefaService);
    }

    @Test
    void ocorrenciasFiltramPorTipo() throws Exception {
        when(execucaoTarefaService.buscarOcorrencias(eq(5L), any(), anyInt(), anyInt())).thenReturn(Page.empty());
        mvc.perform(get("/execucoes-tarefa/5/ocorrencias").param("tipo", "ERRO")
                        .header("Authorization", token(false, true)))
                .andExpect(status().isOk());
        verify(execucaoTarefaService).buscarOcorrencias(5L, TipoOcorrenciaExecucao.ERRO, 0, 20);
    }

    @Test
    void execucaoInexistenteDa404() throws Exception {
        when(execucaoTarefaService.buscarOcorrencias(any(), any(), anyInt(), anyInt()))
                .thenThrow(new ObjectNotFoundException("nao existe"));
        mvc.perform(get("/execucoes-tarefa/99/ocorrencias").header("Authorization", token(false, true)))
                .andExpect(status().isNotFound());
    }
}
