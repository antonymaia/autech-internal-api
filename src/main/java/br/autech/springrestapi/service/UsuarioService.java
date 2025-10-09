package br.autech.springrestapi.service;

import br.autech.springrestapi.model.Usuario;
import br.autech.springrestapi.repository.UsuarioRepository;
import br.autech.springrestapi.service.exception.DataIntegretyException;
import br.autech.springrestapi.service.exception.ObjectNotFoundException;
import br.autech.springrestapi.service.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder encoder;

    public Usuario login(Usuario usuarioLogin) {
        Usuario usuarioProcura = usuarioRepository.findByLogin(usuarioLogin.getLogin());
        if(usuarioProcura == null){
            throw new ObjectNotFoundException("Usuário não encontrado");
        }
        if(!encoder.matches(usuarioLogin.getSenha(), usuarioProcura.getSenha())){
            throw new UnauthorizedException("Senha inválida");
        }

        return usuarioProcura;
    }

    public Usuario insert(Usuario usuario) {
       if(usuarioRepository.findByLogin(usuario.getLogin()) != null){
           throw new DataIntegretyException("Usuário já cadastrado");
       }

        usuario.setSenha(encoder.encode(usuario.getSenha()));
       usuario.setCreatedAt(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }
}
