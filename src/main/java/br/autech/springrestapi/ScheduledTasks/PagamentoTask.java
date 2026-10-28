package br.autech.springrestapi.ScheduledTasks;

import br.autech.springrestapi.model.Fatura;
import br.autech.springrestapi.service.FaturaService;
import br.autech.springrestapi.service.FileService;
import br.autech.springrestapi.service.NcmService;
import br.autech.springrestapi.service.WhatsAppService;
import br.autech.springrestapi.service.WhatsAppService.AvisoFatura;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PagamentoTask {

   private final NcmService ncmService;
   private final FileService fileService;
   private final WhatsAppService whatsAppService;
   private final FaturaService faturaService;

   private static final ZoneId BRASIL = ZoneId.of("America/Sao_Paulo");

   // Fecha as faturas que vencem em 3 dias, cria a do proximo mes e avisa o cliente
   @Scheduled(cron = "0 0 5 * * *")
   public void fecharFaturas() {
      log.info("[Fatura - Fechamento] Inicializando...");
      List<Fatura> fechadas;
      try {
         fechadas = faturaService.fecharFaturas();
      } catch (Exception e) {
         log.error("[Fatura - Fechamento] Erro: {}", e.getMessage(), e);
         return;
      }
      enviarAvisos("Fatura - Fechamento", fechadas, AvisoFatura.FECHAMENTO);
   }

   @Scheduled(cron = "0 10 5 * * *")
   public void marcarFaturasVencidas() {
      log.info("[Fatura - Marcar vencidas] Inicializando...");
      try {
         faturaService.atualizarEstadoFatura();
         log.info("[Fatura - Marcar vencidas] Concluido.");
      } catch (Exception e) {
         log.error("[Fatura - Marcar vencidas] Erro: {}", e.getMessage(), e);
      }
   }

   @Scheduled(cron = "0 20 5 * * *")
   public void bloquearInadimplentes() {
      log.info("[Fatura - Bloqueio D+10] Inicializando...");
      try {
         faturaService.bloquearInadimplentes();
      } catch (Exception e) {
         log.error("[Fatura - Bloqueio D+10] Erro: {}", e.getMessage(), e);
      }
   }

   @Scheduled(cron = "0 0 6 * * *")
   public void enviarAvisosCobrancaDiaDoVencimento() {
      LocalDate hoje = LocalDate.now(BRASIL);
      enviarAvisos("WhatsApp - Aviso no dia do vencimento",
         faturaService.buscarPendentesComVencimentoEm(hoje), AvisoFatura.DIA_VENCIMENTO);
   }

   @Scheduled(cron = "0 30 6 * * *")
   public void enviarAvisosCobranca1DiaAntes() {
      LocalDate amanha = LocalDate.now(BRASIL).plusDays(1);
      enviarAvisos("WhatsApp - Aviso 1 dia antes do vencimento",
         faturaService.buscarPendentesComVencimentoEm(amanha), AvisoFatura.VESPERA);
   }

   @Scheduled(cron = "0 0 7 * * *")
   public void enviarAvisosCobranca2DiasApos() {
      LocalDate anteontem = LocalDate.now(BRASIL).minusDays(2);
      enviarAvisos("WhatsApp - Aviso 2 dias apos o vencimento",
         faturaService.buscarPendentesComVencimentoEm(anteontem), AvisoFatura.ATRASO);
   }

   // Rele cada fatura antes de enviar: se foi paga nesse meio tempo, nao manda
   private void enviarAvisos(String contexto, List<Fatura> faturas, AvisoFatura aviso) {
      log.info("[{}] Inicializando... {} fatura(s).", contexto, faturas.size());
      int enviadas = 0;
      int ignoradas = 0;

      for (Fatura fatura : faturas) {
         try {
            Optional<Fatura> pendente = faturaService.buscarPendenteParaAviso(fatura.getId());
            if (pendente.isEmpty()) {
               ignoradas++;
               continue;
            }
            if (whatsAppService.enviarAvisoFatura(pendente.get(), aviso)) enviadas++;

            Thread.sleep(10000);

         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Processo interrompido.", e);
            break;
         } catch (Exception e) {
            log.error("[{}] Erro ao enviar aviso da fatura {}", contexto, fatura.getId(), e);
         }
      }

      log.info("[{}] Finalizado. Enviadas={}, ignoradas (paga/inativa)={}, falhas={}",
         contexto, enviadas, ignoradas, faturas.size() - enviadas - ignoradas);
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
