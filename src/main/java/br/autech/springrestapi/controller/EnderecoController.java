package br.autech.springrestapi.controller;

import br.autech.springrestapi.model.Endereco;
import br.autech.springrestapi.service.EnderecoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/endereco")
public class EnderecoController {

    @Autowired
    private EnderecoService enderecoService;

    @GetMapping
    public ResponseEntity<List<Endereco>> findAll(){
        return ResponseEntity.ok().body(enderecoService.findAll());
    }

    @RequestMapping("/search")
    public ResponseEntity<Page<Endereco>> search(
            @RequestParam(value = "searchTerm", required = false, defaultValue = "")  String searchTerm,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam("size") int size
    ){
        return ResponseEntity.ok().body(enderecoService.search(searchTerm, page, size));
    }

    @PutMapping
    public ResponseEntity<Void> update(@RequestBody Endereco endereco){
        enderecoService.update(endereco);
        return ResponseEntity.ok().build();
    }
}
