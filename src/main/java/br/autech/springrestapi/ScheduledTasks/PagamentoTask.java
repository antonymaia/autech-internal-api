package br.autech.springrestapi.ScheduledTasks;

import br.autech.springrestapi.dtos.ClienteDTO;
import br.autech.springrestapi.model.Cliente;
import br.autech.springrestapi.service.ClienteService;
import br.autech.springrestapi.service.FaturaService;
import br.autech.springrestapi.service.FileService;
import br.autech.springrestapi.service.NcmService;
import br.autech.springrestapi.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PagamentoTask {

    private final ClienteService clienteService;
    private final FaturaService faturaService;
    private final NcmService ncmService;
    private final FileService fileService;
    private final WhatsAppService whatsAppService;

    private static final ZoneId BRASIL = ZoneId.of("America/Sao_Paulo");

    @Scheduled(cron = "0 30 6 * * *")
    public void enviarAvisosCobranca1DiaAntes() {
        log.info("[WhatsApp - Aviso 1 dia antes do vencimento] Inicializando...");
        LocalDate amanha = LocalDate.now(BRASIL).plusDays(1);
        List<ClienteDTO> clientes = clienteService.buscarClientesPorDiaVencimento(amanha.getDayOfMonth(), "S");
        clientes.forEach(cliente -> whatsAppService.enviarAvisoCobranca(cliente, amanha));
        log.info("[WhatsApp - Aviso 1 dia antes do vencimento] finalizado com sucesso. {} cliente(s) processado(s).", clientes.size());
    }

    @Scheduled(cron = "0 0 6 * * *")
    public void enviarAvisosCobrancaDiaDoVencimento() {
        log.info("[WhatsApp - Aviso no dia do vencimento] Inicializando...");
        LocalDate hoje = LocalDate.now(BRASIL);
        List<ClienteDTO> clientes = clienteService.buscarClientesPorDiaVencimento(hoje.getDayOfMonth(), "S");
        clientes.forEach(cliente -> whatsAppService.enviarAvisoCobrancaDia(cliente, hoje));
        log.info("[WhatsApp - Aviso no dia do vencimento] finalizado com sucesso. {} cliente(s) processado(s).", clientes.size());
    }

    @Scheduled(cron = "0 0 5 * * *")
    public void atualizarNcm() throws IOException {
        try {
            log.info("[Atualizar ncm] Inicializando...");
            ncmService.atualizarNcms();
            log.info("[Atualizar ncm] finalizado com sucesso...");
        } catch (Exception e) {
            fileService.gerarLog("Scheduled Atualizar NCMs", e.getMessage());
        }
    }
}
