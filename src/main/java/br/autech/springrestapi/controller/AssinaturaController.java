package br.autech.springrestapi.controller;

import br.autech.springrestapi.dtos.AssinaturaDTO;
import br.autech.springrestapi.dtos.AssinaturaProdutoDTO;
import br.autech.springrestapi.model.Assinatura;
import br.autech.springrestapi.model.AssinaturaProduto;
import br.autech.springrestapi.service.AssinaturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/assinatura")
public class AssinaturaController {
    @Autowired
    private AssinaturaService assinaturaService;


    @GetMapping
    public ResponseEntity<Assinatura> buscarAssinatura(@RequestParam String cnpjCpf) throws Exception {
        Assinatura assinatura = assinaturaService.buscarAssinatura(cnpjCpf);
        return ResponseEntity.ok().body(assinatura);
    }

    @PostMapping
    public ResponseEntity<?> inserirAssinatura(@RequestBody AssinaturaDTO assinatura) {
        Assinatura assinaturaSalva = assinaturaService.inserirAssinatura(assinatura);

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(assinaturaSalva.getId()).toUri();

        return ResponseEntity.created(uri).build();
    }

    @PutMapping
    public ResponseEntity<?> atualizarAssinatura(@RequestBody AssinaturaDTO assinaturaDTO) {
        Assinatura assinatura = assinaturaService.atualizarAssinatura(assinaturaDTO);
        return ResponseEntity.ok(assinatura);
    }

    @PostMapping("/{id}/produtos")
    public ResponseEntity<AssinaturaProduto> adicionarProduto(@PathVariable Long id,
                                                              @RequestBody AssinaturaProdutoDTO dto) {
        return ResponseEntity.ok(assinaturaService.adicionarProduto(id, dto));
    }

    @DeleteMapping("/produtos/{assinaturaProdutoId}")
    public ResponseEntity<Void> removerProduto(@PathVariable Long assinaturaProdutoId) {
        assinaturaService.removerProduto(assinaturaProdutoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/produtos")
    public ResponseEntity<List<AssinaturaProduto>> listarProdutos(@PathVariable Long id) {
        return ResponseEntity.ok(assinaturaService.listarProdutos(id));
    }
}
