package br.autech.springrestapi.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class FileService {

    public File findById(Long id) {
        String path = "C:\\arquivos-autech-site\\" + id +".zip";
        return new File(path);
    }

    public static String verificarMIME(File file){
        if(file.getName().endsWith(".zip")){
            return "application/zip";
        }
        return "application/json";
    }

    public void gerarLog(String action, String mensagem) throws IOException {
        Path diretorio = Paths.get("C:/internal-admin-api/logs");
        if(!Files.exists(diretorio)){
            Files.createDirectories(diretorio);
        }

        Path arquivo = Paths.get("C:/internal-admin-api/log.txt");
        if(!Files.exists(arquivo)){
            Files.createFile(arquivo);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(arquivo.toString(), true))) {
            writer.write(
                    "data_hora: " + LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).toString()
                            + "\naction: " + action
                            +"\nmensagem: " + mensagem
                            +"\n\n"
            );
        } catch (IOException er) {
            throw new IOException("Erro ao escrever no arquivo.", er);
        }
    }
}
