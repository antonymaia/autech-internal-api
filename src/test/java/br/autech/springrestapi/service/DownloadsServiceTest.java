package br.autech.springrestapi.service;

import br.autech.springrestapi.service.exception.BadRequestException;
import br.autech.springrestapi.service.exception.ConflitoException;
import br.autech.springrestapi.service.exception.InternalServerError;
import br.autech.springrestapi.service.exception.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DownloadsServiceTest {

    @TempDir
    Path base;
    Path raiz;
    DownloadsService service;

    @BeforeEach
    void preparar() throws Exception {
        raiz = Files.createDirectories(base.resolve("downloads"));
        Files.createDirectories(raiz.resolve("apps/android"));
        service = new DownloadsService(raiz.toString());
    }

    private static MockMultipartFile arquivo(String nome, String conteudo) {
        return new MockMultipartFile("arquivo", nome, "application/octet-stream", conteudo.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void enviaArquivoNovoNaPasta() throws Exception {
        assertFalse(service.salvar("apps/android", arquivo("app.apk", "v1"), false, "teste"));
        assertEquals("v1", Files.readString(raiz.resolve("apps/android/app.apk")));
    }

    @Test
    void enviaNaRaizComPastaVazia() throws Exception {
        service.salvar("", arquivo("manual.pdf", "x"), false, "teste");
        assertTrue(Files.exists(raiz.resolve("manual.pdf")));
    }

    @Test
    void recusaArquivoExistenteSemSubstituir() throws Exception {
        service.salvar("apps/android", arquivo("app.apk", "v1"), false, "teste");
        assertThrows(ConflitoException.class, () -> service.salvar("apps/android", arquivo("app.apk", "v2"), false, "teste"));
        assertEquals("v1", Files.readString(raiz.resolve("apps/android/app.apk")));
    }

    @Test
    void substituiArquivoExistenteSemDeixarTemporario() throws Exception {
        service.salvar("apps/android", arquivo("app.apk", "v1"), false, "teste");
        assertTrue(service.salvar("apps/android", arquivo("app.apk", "v2"), true, "teste"));
        assertEquals("v2", Files.readString(raiz.resolve("apps/android/app.apk")));
        try (Stream<Path> itens = Files.list(raiz.resolve("apps/android"))) {
            assertEquals(1, itens.count());
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void arquivoGravadoFicaLegivelParaONginx() throws Exception {
        service.salvar("apps/android", arquivo("app.apk", "v1"), false, "teste");
        assertEquals("rw-r--r--", PosixFilePermissions.toString(
                Files.getPosixFilePermissions(raiz.resolve("apps/android/app.apk"))));
        service.salvar("apps/android", arquivo("app.apk", "v2"), true, "teste");
        assertEquals("rw-r--r--", PosixFilePermissions.toString(
                Files.getPosixFilePermissions(raiz.resolve("apps/android/app.apk"))));
    }

    @Test
    void substituirArquivoQueNaoExisteCriaNovo() throws Exception {
        assertFalse(service.salvar("apps/android", arquivo("novo.apk", "v1"), true, "teste"));
        assertTrue(Files.exists(raiz.resolve("apps/android/novo.apk")));
    }

    @Test
    void recusaPastaForaDaRaiz() {
        assertThrows(BadRequestException.class, () -> service.salvar("../", arquivo("x.txt", "x"), false, "teste"));
        assertThrows(BadRequestException.class, () -> service.salvar("apps/../../fora", arquivo("x.txt", "x"), false, "teste"));
        assertThrows(BadRequestException.class, () -> service.salvar(base.toString(), arquivo("x.txt", "x"), false, "teste"));
        assertFalse(Files.exists(base.resolve("x.txt")));
    }

    @Test
    void recusaPastaInexistente() {
        assertThrows(ObjectNotFoundException.class, () -> service.salvar("nao-existe", arquivo("x.txt", "x"), false, "teste"));
    }

    @Test
    void usaSoONomeDoArquivoEnviado() throws Exception {
        service.salvar("", arquivo("../../fora.txt", "x"), false, "teste");
        assertTrue(Files.exists(raiz.resolve("fora.txt")));
        assertFalse(Files.exists(base.resolve("fora.txt")));
    }

    @Test
    void recusaNomesInvalidos() {
        for (String nome : new String[]{"", ".oculto", "pagina.html", "imagem.SVG", "a:b.txt", "pasta/", "fim."}) {
            assertThrows(BadRequestException.class, () -> service.salvar("", arquivo(nome, "x"), false, "teste"), nome);
        }
    }

    @Test
    void semRaizConfiguradaNaoGrava() {
        DownloadsService semRaiz = new DownloadsService("");
        assertThrows(InternalServerError.class, () -> semRaiz.salvar("", arquivo("x.txt", "x"), false, "teste"));
    }
}
