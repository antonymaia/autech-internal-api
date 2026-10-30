package br.autech.springrestapi.service;

import br.autech.springrestapi.service.exception.BadRequestException;
import br.autech.springrestapi.service.exception.ConflitoException;
import br.autech.springrestapi.service.exception.InternalServerError;
import br.autech.springrestapi.service.exception.ObjectNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Publica arquivos na pasta servida pelo nginx de downloads.autech.net.br (listagem em /api/arquivos/, download em
 * /downloads/). Só grava dentro de downloads.raiz.
 */
@Slf4j
@Service
public class DownloadsService {

    private static final Pattern CARACTERE_INVALIDO = Pattern.compile("[\\\\/:*?\"<>|\\x00-\\x1F]");
    /** Abririam como página no domínio de downloads em vez de baixar. */
    private static final Set<String> EXTENSOES_BLOQUEADAS = Set.of("html", "htm", "xhtml", "svg", "js");
    private static final boolean POSIX = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    private static final Set<PosixFilePermission> PERMISSAO_ARQUIVO = PosixFilePermissions.fromString("rw-r--r--");

    private final String raiz;

    public DownloadsService(@Value("${downloads.raiz:}") String raiz) {
        this.raiz = raiz;
    }

    /**
     * Grava o arquivo em `pasta` (relativa à raiz, ex.: "apps/android"; vazia = raiz).
     *
     * @return true se substituiu um arquivo que já existia.
     */
    public boolean salvar(String pasta, MultipartFile arquivo, boolean substituir, String login) throws IOException {
        String nome = validarNome(arquivo.getOriginalFilename());
        Path raizReal = raizReal();
        Path diretorio;
        try {
            diretorio = raizReal.resolve(pasta == null ? "" : pasta).normalize();
        } catch (InvalidPathException e) {
            throw new BadRequestException("Pasta inválida: " + pasta);
        }
        // Barra o "../" e o caminho absoluto ("/etc", "C:/...")
        if (!diretorio.startsWith(raizReal)) throw new BadRequestException("Pasta inválida: " + pasta);
        if (!Files.isDirectory(diretorio)) throw new ObjectNotFoundException("Pasta não encontrada: " + pasta);
        // Barra o atalho (symlink) que aponta para fora da raiz
        if (!diretorio.toRealPath().startsWith(raizReal)) throw new BadRequestException("Pasta inválida: " + pasta);

        Path destino = diretorio.resolve(nome);
        boolean existia = Files.exists(destino);
        if (existia && !substituir) throw new ConflitoException("Já existe um arquivo com esse nome nesta pasta");
        if (Files.isDirectory(destino)) throw new ConflitoException("Já existe uma pasta com esse nome");

        // Grava ao lado do destino com nome oculto (o autoindex do nginx não lista arquivos com ponto na frente) e só
        // depois troca: quem estiver baixando a versão antiga não recebe um arquivo pela metade.
        Path temporario = Files.createTempFile(diretorio, ".envio-", ".tmp");
        try {
            arquivo.transferTo(temporario);
            // O temporário nasce rw------- (só o dono): sem isso o nginx, que roda com outro usuário, responde 403
            if (POSIX) Files.setPosixFilePermissions(temporario, PERMISSAO_ARQUIVO);
            if (substituir) {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } else {
                // Sem REPLACE_EXISTING: falha se outro envio criou o mesmo nome nesse meio-tempo
                Files.move(temporario, destino);
            }
        } catch (FileAlreadyExistsException e) {
            throw new ConflitoException("Já existe um arquivo com esse nome nesta pasta");
        } finally {
            Files.deleteIfExists(temporario);
        }

        log.info("[Downloads] {} {} {} ({} bytes)", login, existia ? "substituiu" : "enviou",
                raizReal.relativize(destino), arquivo.getSize());
        return existia;
    }

    private Path raizReal() throws IOException {
        if (raiz == null || raiz.isBlank()) {
            throw new InternalServerError("Pasta de downloads não configurada (downloads.raiz / DOWNLOADS_RAIZ)");
        }
        return Paths.get(raiz).toRealPath();
    }

    private String validarNome(String original) {
        String nome = original == null ? ""
                : original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1).trim();
        String extensao = nome.contains(".") ? nome.substring(nome.lastIndexOf('.') + 1).toLowerCase() : "";
        if (nome.isEmpty() || nome.startsWith(".") || nome.endsWith(".") || nome.length() > 200
                || CARACTERE_INVALIDO.matcher(nome).find() || EXTENSOES_BLOQUEADAS.contains(extensao)) {
            throw new BadRequestException("Nome de arquivo inválido: " + original);
        }
        return nome;
    }
}
