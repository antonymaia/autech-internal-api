package br.autech.springrestapi.controller;

import br.autech.springrestapi.dtos.AssinaturaDTO;
import br.autech.springrestapi.model.Assinatura;
import br.autech.springrestapi.model.Cidade;
import br.autech.springrestapi.model.Cliente;
import br.autech.springrestapi.model.enums.StatusAssinatura;
import br.autech.springrestapi.model.enums.TipoAssinatura;
import br.autech.springrestapi.service.AssinaturaService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/assinatura")
public class AssinaturaController {
 @Autowired
 private AssinaturaService assinaturaService;




    @GetMapping
    public ResponseEntity<AssinaturaDTO> buscarAssinatura(@RequestParam String cnpjCpf ) throws Exception {
       AssinaturaDTO assinatura = assinaturaService.buscarAssinatura(cnpjCpf);
        return ResponseEntity.ok().body(assinatura);
    }

    @PostMapping
    public ResponseEntity<?> inserirAssinatura (@RequestBody AssinaturaDTO assinatura) {


    Assinatura assinaturaSalva =   assinaturaService.inserirAssinatura(assinatura);

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(assinaturaSalva.getId()).toUri();

        return ResponseEntity.created(uri).body(assinaturaSalva)    ;
    }

    @PutMapping
    public ResponseEntity<?> atualizarAssinatura ( @RequestBody AssinaturaDTO assinaturaDTO){
      Assinatura assinatura =  assinaturaService.atualizarAssinatura(assinaturaDTO);
      System.out.println("assinatura: "+assinatura);
        return ResponseEntity.ok(assinatura);
    }



}
