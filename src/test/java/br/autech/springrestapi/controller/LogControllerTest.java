package br.autech.springrestapi.controller;

import br.autech.springrestapi.config.PortalRateLimitInterceptor;
import br.autech.springrestapi.config.SecurityConfig;
import br.autech.springrestapi.model.Usuario;
import br.autech.springrestapi.repository.AssinaturaRepository;
import br.autech.springrestapi.service.AssinaturaService;
import br.autech.springrestapi.service.ClienteService;
import br.autech.springrestapi.service.TokenService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LogController.class)
@Import({SecurityConfig.class, TokenService.class})
@TestPropertySource(properties = "jwt.secret=c2VncmVkby1kZS10ZXN0ZS1jb20tbWFpcy1kZS0zMi1ieXRlcy0xMjM0NTY=")
class LogControllerTest {

    private static final String[] ROTAS = {
            "/logs/whatsapp-cobranca", "/logs/whatsapp-cobranca/download", "/logs/whatsapp-cobranca/arquivos"};

    @TempDir
    static Path raiz;

    @Autowired
    MockMvc mvc;
    @Autowired
    TokenService tokenService;
    @MockBean
    PortalRateLimitInterceptor portalRateLimitInterceptor;
    // Injetados na classe App (carregada pelo @WebMvcTest)
    @MockBean
    ClienteService clienteService;
    @MockBean
    AssinaturaRepository assinaturaRepository;
    @MockBean
    AssinaturaService assinaturaService;

    @DynamicPropertySource
    static void diretorioDeLog(DynamicPropertyRegistry registro) {
        registro.add("log.cobranca.dir", () -> raiz.resolve("log").toString());
    }

    @BeforeAll
    static void criarArquivos() throws IOException {
        Path log = Files.createDirectories(raiz.resolve("log"));
        Files.writeString(log.resolve("log-cobranca-whatsapp.log"), "linha atual");
        Files.writeString(log.resolve("log-cobranca-whatsapp-2026-11-01.log"), "linha antiga");
        Files.writeString(raiz.resolve("segredo.log"), "fora da pasta de log");
    }

    private String token(boolean podePublicar, boolean podeVerExecucoes) {
        Usuario usuario = new Usuario(1L, "fulano", null, null, null);
        usuario.setPodePublicar(podePublicar);
        usuario.setPodeVerExecucoes(podeVerExecucoes);
        return "Bearer " + tokenService.gerar(usuario);
    }

    @Test
    void semTokenDa401EmTodasAsRotas() throws Exception {
        for (String rota : ROTAS) {
            mvc.perform(get(rota)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void tokenDoPortalDoClienteDa401() throws Exception {
        for (String rota : ROTAS) {
            mvc.perform(get(rota).header("Authorization", "Bearer token-do-portal-do-cliente"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void semAPermissaoDa403EmTodasAsRotas() throws Exception {
        for (String rota : ROTAS) {
            mvc.perform(get(rota).header("Authorization", token(true, false))).andExpect(status().isForbidden());
        }
    }

    @Test
    void comPermissaoLeOLogAtualEOPorData() throws Exception {
        mvc.perform(get("/logs/whatsapp-cobranca").header("Authorization", token(false, true)))
                .andExpect(status().isOk())
                .andExpect(content().string("linha atual"));
        mvc.perform(get("/logs/whatsapp-cobranca").param("data", "2026-11-01").header("Authorization", token(false, true)))
                .andExpect(status().isOk())
                .andExpect(content().string("linha antiga"));
    }

    @Test
    void comPermissaoBaixaEListaOsArquivos() throws Exception {
        mvc.perform(get("/logs/whatsapp-cobranca/download").param("data", "2026-11-01")
                        .header("Authorization", token(false, true)))
                .andExpect(status().isOk());
        mvc.perform(get("/logs/whatsapp-cobranca/arquivos").header("Authorization", token(false, true)))
                .andExpect(status().isOk());
    }

    @Test
    void dataForaDoFormatoDa400EmVezDeSairDaPastaDeLog() throws Exception {
        String[] datas = {"x/../../segredo", "x\\..\\..\\segredo", "2026-13-40", "ontem"};
        for (String data : datas) {
            mvc.perform(get("/logs/whatsapp-cobranca").param("data", data).header("Authorization", token(false, true)))
                    .andExpect(status().isBadRequest());
            mvc.perform(get("/logs/whatsapp-cobranca/download").param("data", data)
                            .header("Authorization", token(false, true)))
                    .andExpect(status().isBadRequest());
        }
    }
}
