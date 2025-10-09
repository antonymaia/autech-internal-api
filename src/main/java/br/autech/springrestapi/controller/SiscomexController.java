package br.autech.springrestapi.controller;

import br.autech.springrestapi.dtos.TabelaNcmSiscomexDTO;
import br.autech.springrestapi.service.SiscomexService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@CrossOrigin("*")
@RestController
@RequestMapping("/siscomex")
public class SiscomexController {

    private final SiscomexService siscomexService;

    public SiscomexController(SiscomexService siscomexService) {
        this.siscomexService = siscomexService;
    }

    @GetMapping("/ncm")
    public TabelaNcmSiscomexDTO getNcmData() {
        return siscomexService.buscarNcm();
    }
}
