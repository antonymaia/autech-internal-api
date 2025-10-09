package br.autech.springrestapi.ScheduledTasks;

import br.autech.springrestapi.model.Cliente;
import br.autech.springrestapi.service.ClienteService;
import br.autech.springrestapi.service.FaturaService;
import br.autech.springrestapi.service.FileService;
import br.autech.springrestapi.service.NcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PagamentoTask {

    private final ClienteService clienteService;
    private final FaturaService faturaService;
    private final NcmService ncmService;
    private final FileService fileService;

    @Scheduled(cron = "0 0 6 * * *")
    public void enviarEmailClienteLicencaVencida(){
        log.info("[Enviando emails para licenças que expiram hoje] Inicializando...");
        LocalDate date = LocalDate.now();
        List<Cliente> clientes = clienteService.buscarClientesPorDiaVencimentoAtivo(date.getDayOfMonth(), "S");
        clientes.forEach(clienteService::enviarEmailLicencaVencida);
        log.info("[Enviando emails para licenças que expiram hoje] finalizado com sucesso.");
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void gerarFaturaCliente(){
        log.info("[Enviando emails para licenças que expiram em 5 dias] Inicializando...");
        faturaService.gerarFaturas5dias();
        log.info("[Enviando emails para licenças que expiram em 5 dias] finalizado com sucesso");
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void atualizarEstadoFatura(){
        log.info("[Atualizar estado das faturas] Inicializando...");
        faturaService.atualizarEstadoFatura();
        log.info("[Atualizar estado das faturas] finalizado com sucesso...");
    }

    @Scheduled(cron = "0 0 5 * * *")
    public void atualizarNcm() throws IOException {
        try{
            log.info("[Atualizar ncm] Inicializando...");
            ncmService.atualizarNcms();
            log.info("[Atualizar ncm] finalizado com sucesso...");
        } catch (Exception e){
            fileService.gerarLog("Scheduled Atualizar NCMs", e.getMessage());
        }
    }
}
