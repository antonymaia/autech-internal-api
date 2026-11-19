package br.autech.springrestapi.controller;

import br.autech.springrestapi.config.PortalRateLimitInterceptor;
import br.autech.springrestapi.config.SecurityConfig;
import br.autech.springrestapi.dtos.ClienteNuvemDTO;
import br.autech.springrestapi.model.Usuario;
import br.autech.springrestapi.repository.AssinaturaRepository;
import br.autech.springrestapi.service.AssinaturaService;
import br.autech.springrestapi.service.ClienteService;
import br.autech.springrestapi.service.FaturaService;
import br.autech.springrestapi.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClienteController.class)
@Import({SecurityConfig.class, TokenService.class})
@TestPropertySource(properties = {
        "jwt.secret=c2VncmVkby1kZS10ZXN0ZS1jb20tbWFpcy1kZS0zMi1ieXRlcy0xMjM0NTY=",
        "api.chaves=nuvem:" + ClienteNuvemControllerTest.CHAVE_NUVEM + ", app-local:" + ClienteNuvemControllerTest.CHAVE_APP
})
class ClienteNuvemControllerTest {

    static final String CHAVE_NUVEM = "0123456789abcdef0123456789abcdef";
    static final String CHAVE_APP = "fedcba9876543210fedcba9876543210";

    @Autowired
    MockMvc mvc;
    @Autowired
    TokenService tokenService;
    @MockBean
    ClienteService clienteService;
    @MockBean
    FaturaService faturaService;
    @MockBean
    PortalRateLimitInterceptor portalRateLimitInterceptor;
    // Injetados na classe App (carregada pelo @WebMvcTest)
    @MockBean
    AssinaturaRepository assinaturaRepository;
    @MockBean
    AssinaturaService assinaturaService;

    @Test
    void semChaveDa401() throws Exception {
        mvc.perform(get("/cliente/nuvem")).andExpect(status().isUnauthorized());
        verify(clienteService, never()).listarClientesNuvem();
    }

    @Test
    void chaveErradaDa401() throws Exception {
        mvc.perform(get("/cliente/nuvem").header("X-API-Key", CHAVE_NUVEM + "x"))
                .andExpect(status().isUnauthorized());
        verify(clienteService, never()).listarClientesNuvem();
    }

    @Test
    void tokenDeColaboradorNaoSubstituiAChave() throws Exception {
        Usuario usuario = new Usuario(1L, "fulano", null, null, null);
        usuario.setPodePublicar(true);
        usuario.setPodeVerExecucoes(true);
        mvc.perform(get("/cliente/nuvem").header("Authorization", "Bearer " + tokenService.gerar(usuario)))
                .andExpect(status().isForbidden());
        verify(clienteService, never()).listarClientesNuvem();
    }

    @Test
    void qualquerChaveCadastradaListaOsClientes() throws Exception {
        when(clienteService.listarClientesNuvem())
                .thenReturn(List.of(new ClienteNuvemDTO("47831223000164", "NOME DO CLIENTE")));

        for (String chave : List.of(CHAVE_NUVEM, CHAVE_APP)) {
            mvc.perform(get("/cliente/nuvem").header("X-API-Key", chave))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].cnpjCpf").value("47831223000164"))
                    .andExpect(jsonPath("$[0].nome").value("NOME DO CLIENTE"))
                    .andExpect(jsonPath("$[0].length()").value(2));
        }
    }

    @Test
    void demaisRotasDoClienteContinuamAbertas() throws Exception {
        mvc.perform(get("/cliente/count/ativo/S")).andExpect(status().isOk());
    }
}
