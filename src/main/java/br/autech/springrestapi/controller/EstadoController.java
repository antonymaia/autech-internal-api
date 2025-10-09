package br.autech.springrestapi.controller;

import br.autech.springrestapi.model.Estado;
import br.autech.springrestapi.repository.EstadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/estado")
public class EstadoController {

    @Autowired
    private EstadoRepository estadoRepository;

    @RequestMapping
    public ResponseEntity<List<Estado>> findAll(){
        return ResponseEntity.ok().body(estadoRepository.findAll());
    }
}
