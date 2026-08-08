package br.autech.springrestapi.controller;

import br.autech.springrestapi.dtos.TipoPagamentoDTO;
import br.autech.springrestapi.service.PagamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pagamento")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @GetMapping("/tipos")
    public ResponseEntity<List<TipoPagamentoDTO>> listarTipos() {
        return ResponseEntity.ok(pagamentoService.listarTipos());
    }
}
