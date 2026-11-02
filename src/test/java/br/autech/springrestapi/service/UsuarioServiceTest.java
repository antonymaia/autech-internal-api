package br.autech.springrestapi.service;

import br.autech.springrestapi.model.Usuario;
import br.autech.springrestapi.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {

    @Test
    void cadastroNaoConsegueSeDarPermissoes() {
        UsuarioRepository repository = mock(UsuarioRepository.class);
        when(repository.save(any())).then(returnsFirstArg());
        UsuarioService service = new UsuarioService(repository, mock(PasswordEncoder.class));
        Usuario usuario = new Usuario(null, "fulano", "senha", null, null);
        usuario.setPodePublicar(true);
        usuario.setPodeVerExecucoes(true);

        Usuario salvo = service.insert(usuario);

        assertFalse(salvo.getPodePublicar());
        assertFalse(salvo.getPodeVerExecucoes());
    }
}
