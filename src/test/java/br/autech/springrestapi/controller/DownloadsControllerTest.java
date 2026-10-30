package br.autech.springrestapi.controller;

import br.autech.springrestapi.config.PortalRateLimitInterceptor;
import br.autech.springrestapi.config.SecurityConfig;
import br.autech.springrestapi.model.Usuario;
import br.autech.springrestapi.repository.AssinaturaRepository;
import br.autech.springrestapi.service.AssinaturaService;
import br.autech.springrestapi.service.ClienteService;
import br.autech.springrestapi.service.DownloadsService;
import br.autech.springrestapi.service.TokenService;
import br.autech.springrestapi.service.exception.ConflitoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DownloadsController.class)
@Import({SecurityConfig.class, TokenService.class})
@TestPropertySource(properties = "jwt.secret=c2VncmVkby1kZS10ZXN0ZS1jb20tbWFpcy1kZS0zMi1ieXRlcy0xMjM0NTY=")
class DownloadsControllerTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    TokenService tokenService;
    @MockBean
    DownloadsService downloadsService;
    @MockBean
    PortalRateLimitInterceptor portalRateLimitInterceptor;
    // Injetados na classe App (carregada pelo @WebMvcTest)
    @MockBean
    ClienteService clienteService;
    @MockBean
    AssinaturaRepository assinaturaRepository;
    @MockBean
    AssinaturaService assinaturaService;

    private String token(boolean podePublicar) {
        Usuario usuario = new Usuario(1L, "fulano", null, null, null);
        usuario.setPodePublicar(podePublicar);
        return tokenService.gerar(usuario);
    }

    private static MockMultipartHttpServletRequestBuilder envio() {
        return (MockMultipartHttpServletRequestBuilder) multipart("/downloads/arquivos")
                .file(new MockMultipartFile("arquivo", "app.apk", "application/octet-stream", new byte[]{1}))
                .param("pasta", "apps/android");
    }

    @Test
    void semTokenDa401() throws Exception {
        mvc.perform(envio()).andExpect(status().isUnauthorized());
        verifyNoInteractions(downloadsService);
    }

    @Test
    void tokenInvalidoDa401() throws Exception {
        mvc.perform(envio().header("Authorization", "Bearer token-do-portal-do-cliente"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void outrasRotasContinuamAbertasMesmoComTokenDoCliente() throws Exception {
        // 404 (e não 401/403): a segurança deixou passar, só não há controller para essa rota neste teste
        mvc.perform(get("/pub/qualquer").header("Authorization", "Bearer token-do-portal-do-cliente"))
                .andExpect(status().isNotFound());
    }

    @Test
    void semPermissaoDa403() throws Exception {
        mvc.perform(envio().header("Authorization", "Bearer " + token(false))).andExpect(status().isForbidden());
        verifyNoInteractions(downloadsService);
    }

    @Test
    void comPermissaoEnvia() throws Exception {
        when(downloadsService.salvar(eq("apps/android"), any(), eq(false), eq("fulano"))).thenReturn(false);
        mvc.perform(envio().header("Authorization", "Bearer " + token(true))).andExpect(status().isCreated());
    }

    @Test
    void substituindoDa200() throws Exception {
        when(downloadsService.salvar(any(), any(), eq(true), any())).thenReturn(true);
        mvc.perform(envio().param("substituir", "true").header("Authorization", "Bearer " + token(true)))
                .andExpect(status().isOk());
    }

    @Test
    void conflitoDa409() throws Exception {
        when(downloadsService.salvar(any(), any(), anyBoolean(), any())).thenThrow(new ConflitoException("existe"));
        mvc.perform(envio().header("Authorization", "Bearer " + token(true))).andExpect(status().isConflict());
    }
}
