package br.autech.springrestapi.controller;

import br.autech.springrestapi.service.NcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin("*")
@RestController
@RequestMapping("ncm")
@RequiredArgsConstructor
public class NcmController {
    private final NcmService ncmService;

    @GetMapping("atualizar")
    ResponseEntity<Void> atualizarNcm() {
        ncmService.atualizarNcms();
        return ResponseEntity.ok().build();
    }
}
