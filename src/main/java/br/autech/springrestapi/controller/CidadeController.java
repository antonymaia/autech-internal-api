package br.autech.springrestapi.controller;

import br.autech.springrestapi.model.Cidade;
import br.autech.springrestapi.repository.CidadeRepositoy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/cidade")
public class CidadeController {

    @Autowired
    private CidadeRepositoy cidadeRepositoy;

    @GetMapping
    public ResponseEntity<List<Cidade>> findAll(){
        return ResponseEntity.ok().body(cidadeRepositoy.findAll());
    }
}
