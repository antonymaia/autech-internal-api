package br.autech.springrestapi.controller;

import br.autech.springrestapi.model.Usuario;
import br.autech.springrestapi.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@Controller
@RequestMapping("/usuario")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;

    @PostMapping("registrar")
    public ResponseEntity<Usuario> insert(@RequestBody Usuario usuario){
        return ResponseEntity.ok().body(usuarioService.insert(usuario));
    }

    @RequestMapping(value ="/login", method =RequestMethod.POST)
    public ResponseEntity<Usuario> validarLogin(@RequestBody Usuario usuario){

        return ResponseEntity.ok().body(usuarioService.login(usuario));
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<Usuario>> findAll(){
        return ResponseEntity.ok().body(usuarioService.findAll());
    }
}
