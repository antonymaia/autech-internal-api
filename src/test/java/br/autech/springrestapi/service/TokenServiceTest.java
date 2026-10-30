package br.autech.springrestapi.service;

import br.autech.springrestapi.model.Usuario;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private static final String SEGREDO = "c2VncmVkby1kZS10ZXN0ZS1jb20tbWFpcy1kZS0zMi1ieXRlcy0xMjM0NTY=";

    private static Usuario usuario(boolean podePublicar) {
        Usuario usuario = new Usuario(7L, "fulano", null, null, null);
        usuario.setPodePublicar(podePublicar);
        return usuario;
    }

    @Test
    void geraEValidaOToken() {
        TokenService service = new TokenService(SEGREDO, 12);
        Claims claims = service.validar(service.gerar(usuario(true)));
        assertNotNull(claims);
        assertEquals("7", claims.getSubject());
        assertEquals("fulano", claims.get("login", String.class));
        assertTrue(claims.get("publicar", Boolean.class));
    }

    @Test
    void semPermissaoOuComNullVaiFalse() {
        TokenService service = new TokenService(SEGREDO, 12);
        Usuario semValor = usuario(false);
        semValor.setPodePublicar(null);
        assertFalse(service.validar(service.gerar(semValor)).get("publicar", Boolean.class));
    }

    @Test
    void recusaTokenDeOutroSegredoExpiradoOuLixo() {
        TokenService service = new TokenService(SEGREDO, 12);
        assertNull(new TokenService("", 12).validar(service.gerar(usuario(true))));
        assertNull(service.validar(new TokenService(SEGREDO, 0).gerar(usuario(true))));
        assertNull(service.validar("nao-e-um-token"));
    }
}
