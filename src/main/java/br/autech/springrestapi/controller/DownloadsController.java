package br.autech.springrestapi.controller;

import br.autech.springrestapi.service.DownloadsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Publicação de arquivos no servidor de downloads. Exige o token de um usuário com PODE_PUBLICAR (SecurityConfig). */
@RestController
@RequestMapping("/downloads")
@RequiredArgsConstructor
public class DownloadsController {

    private final DownloadsService downloadsService;

    /** Envia um arquivo para uma pasta do servidor de downloads: 201 = novo, 200 = substituiu o que existia. */
    @PostMapping(value = "/arquivos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> enviar(@RequestParam(defaultValue = "") String pasta,
                                       @RequestParam MultipartFile arquivo,
                                       @RequestParam(defaultValue = "false") boolean substituir,
                                       Authentication autenticacao) throws IOException {
        boolean substituiu = downloadsService.salvar(pasta, arquivo, substituir, autenticacao.getName());
        return ResponseEntity.status(substituiu ? HttpStatus.OK : HttpStatus.CREATED).build();
    }
}
