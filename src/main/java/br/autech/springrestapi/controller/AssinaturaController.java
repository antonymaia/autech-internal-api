package br.autech.springrestapi.controller;

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
    public ResponseEntity<Assinatura> buscarAssinatura(@RequestParam String cnpjCpf ) throws Exception {
       Assinatura assinatura = assinaturaService.buscarAssinatura(cnpjCpf);
        return ResponseEntity.ok().body(assinatura);
    }

    @PostMapping
    public ResponseEntity<?> inserirAssinatura (@RequestBody Assinatura assinatura) {


       assinaturaService.inserirAssinatura(assinatura, assinatura.getCliente().getCnpjCpf());

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(assinatura.getId()).toUri();

        return ResponseEntity.created(uri).build();
    }

    @PutMapping("/{cnpjCpf}")
    public ResponseEntity<?> atualizarStatusAssinatura (@PathVariable String cnpjCpf,  @RequestBody Assinatura assinatura ){
        assinaturaService.atualizarStatusPorCliente(cnpjCpf, assinatura.getStatus() );
        return ResponseEntity.ok().build();
    }



}
