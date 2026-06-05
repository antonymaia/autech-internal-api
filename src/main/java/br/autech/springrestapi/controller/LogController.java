package br.autech.springrestapi.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/logs")
public class LogController {

    private static final String ARQUIVO_ATUAL = "log-cobranca-whatsapp.log";
    private static final String PREFIXO_ROTACIONADO = "log-cobranca-whatsapp-";

    @Value("${log.cobranca.dir:log}")
    private String logDir;

    @GetMapping(value = "/whatsapp-cobranca", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verLogCobranca(
            @RequestParam(name = "linhas", defaultValue = "200") int linhas,
            @RequestParam(name = "data", required = false) String data) throws IOException {

        Path arquivo = resolverArquivo(data);
        if (!Files.exists(arquivo)) {
            return ResponseEntity.ok("Nenhum log encontrado para o periodo solicitado.");
        }
        List<String> todasLinhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        if (todasLinhas.isEmpty()) {
            return ResponseEntity.ok("Arquivo vazio.");
        }
        int qtd = Math.max(linhas, 1);
        int from = Math.max(0, todasLinhas.size() - qtd);
        String conteudo = String.join("\n", todasLinhas.subList(from, todasLinhas.size()));
        return ResponseEntity.ok(conteudo);
    }

    @GetMapping("/whatsapp-cobranca/download")
    public ResponseEntity<Resource> downloadLogCobranca(
            @RequestParam(name = "data", required = false) String data) {
        Path arquivo = resolverArquivo(data);
        if (!Files.exists(arquivo)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(arquivo.toFile());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + arquivo.getFileName() + "\"")
                .body(resource);
    }

    @GetMapping(value = "/whatsapp-cobranca/arquivos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> listarArquivos() throws IOException {
        Path diretorio = Paths.get(logDir);
        if (!Files.exists(diretorio)) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<String> arquivos = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(diretorio, "log-cobranca-whatsapp*.log")) {
            for (Path p : stream) {
                arquivos.add(p.getFileName().toString());
            }
        }
        arquivos.sort(Comparator.reverseOrder());
        return ResponseEntity.ok(arquivos);
    }

    private Path resolverArquivo(String data) {
        if (data == null || data.isBlank()) {
            return Paths.get(logDir, ARQUIVO_ATUAL);
        }
        return Paths.get(logDir, PREFIXO_ROTACIONADO + data + ".log");
    }
}
