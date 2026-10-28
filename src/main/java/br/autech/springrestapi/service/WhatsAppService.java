package br.autech.springrestapi.service;

import br.autech.springrestapi.model.Cliente;
import br.autech.springrestapi.model.Fatura;
import br.autech.springrestapi.model.enums.EstadoFatura;
import br.autech.springrestapi.repository.FaturaRepository;
import br.autech.springrestapi.service.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {
   private final FaturaRepository faturaRepository;

   private static final Logger LOG_COBRANCA = LoggerFactory.getLogger("cobranca-whatsapp");

   private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
   private static final RestTemplate restTemplate = new RestTemplate();
   private static final ZoneId BRASIL = ZoneId.of("America/Sao_Paulo");

   @Value("${whatsapp.evolution.base-url:}")
   private String baseUrl;
   @Value("${whatsapp.evolution.instance.name:}")
   private String instance;
   @Value("${whatsapp.evolution.api-key:}")
   private String apiKey;
   @Value("${portal.url:https://pagar.autech.net.br}")
   private String portalUrl;

   public enum AvisoFatura {
      FECHAMENTO("fechamento"),
      VESPERA("1 dia antes"),
      DIA_VENCIMENTO("dia do vencimento"),
      ATRASO("2 dias apos o vencimento");

      private final String contexto;

      AvisoFatura(String contexto) {
         this.contexto = contexto;
      }
   }

   public void enviarMensagemTeste(String telefone) {
      if (!isConfigurado()) {
         log.warn("WhatsApp Evolution API não configurada. Mensagem de teste não enviada.");
         return;
      }
      String telefoneFormatado = normalizarTelefone(telefone);
      if (telefoneFormatado == null) {
         log.warn("Telefone inválido para teste: '{}'", telefone);
         return;
      }
      LocalDate vencimento = LocalDate.now(BRASIL).plusDays(1);
      String mensagem = montarMensagem(AvisoFatura.VESPERA, "",
         vencimento.format(FORMATTER), formatarValor(new BigDecimal("135.99")));
      enviar(telefoneFormatado, mensagem, log);
   }

   /**
    * Envia o aviso da fatura para o WhatsApp do cliente. Falhas e dados faltando
    * vao para o log de cobranca. Retorna true se a mensagem foi aceita pela API.
    */
   public boolean enviarAvisoFatura(Fatura fatura, AvisoFatura aviso) {
      String contexto = aviso.contexto;
      if (fatura == null || fatura.getCliente() == null
         || fatura.getValor() == null || fatura.getDataVencimento() == null) {
         LOG_COBRANCA.warn("[{}] Dados invalidos para envio (fatura, cliente, valor ou vencimento nulos). Fatura: {}",
            contexto, fatura != null ? fatura.getId() : null);
         return false;
      }

      Cliente cliente = fatura.getCliente();
      String nomeLog = nomeClienteParaLog(cliente);
      String vencimento = fatura.getDataVencimento().format(FORMATTER);
      String valor = formatarValor(fatura.getValor());

      if (!isConfigurado()) {
         LOG_COBRANCA.warn("[{}] WhatsApp Evolution API nao configurada. Fatura {} do cliente {} ({}) nao notificada. Vencimento: {}, Valor: {}",
            contexto, fatura.getId(), cliente.getCnpjCpf(), nomeLog, vencimento, valor);
         return false;
      }

      String telefone = cliente.getTelefone();
      if (telefone == null || telefone.isBlank()) {
         LOG_COBRANCA.warn("[{}] Cliente {} ({}) sem telefone cadastrado. Fatura {}, Vencimento: {}, Valor: {}",
            contexto, cliente.getCnpjCpf(), nomeLog, fatura.getId(), vencimento, valor);
         return false;
      }

      String telefoneFormatado = normalizarTelefone(telefone);
      if (telefoneFormatado == null) {
         LOG_COBRANCA.warn("[{}] Telefone invalido para cliente {} ({}): '{}'. Fatura {}, Vencimento: {}, Valor: {}",
            contexto, cliente.getCnpjCpf(), nomeLog, telefone, fatura.getId(), vencimento, valor);
         return false;
      }

      String mensagem = montarMensagem(aviso, resolverNomeCliente(cliente), vencimento, valor);

      try {
         ResponseEntity<String> response = doSend(telefoneFormatado, mensagem);
         log.info("WhatsApp [{}] enviado para {} ({}) tel {} - fatura {} - status {}",
            contexto, cliente.getCnpjCpf(), nomeLog, telefoneFormatado,
            fatura.getId(), response.getStatusCode());
         return true;
      } catch (Exception e) {
         LOG_COBRANCA.error("[{}] Falha ao enviar WhatsApp. Cliente: {} ({}), Telefone: {}, Fatura: {}, Vencimento: {}, Valor: {}, Erro: {}",
            contexto, cliente.getCnpjCpf(), nomeLog, telefoneFormatado,
            fatura.getId(), vencimento, valor, e.getMessage());
         return false;
      }
   }

   /**
    * Envio manual: cobra a fatura pendente (fechada ou vencida) mais antiga do cliente.
    */
   public void enviarMensagemCobranca(String cnpjCpf) {
      List<Fatura> pendentes = faturaRepository.findAllByCliente_CnpjCpfAndEstadoInOrderByDataVencimento(
         cnpjCpf, List.of(EstadoFatura.FECHADA.getCod(), EstadoFatura.VENCIDA.getCod()));
      if (pendentes.isEmpty()) {
         throw new BadRequestException("Mensagem não enviada, cliente não possui fatura pendente");
      }

      Fatura fatura = pendentes.get(0);
      LocalDate hoje = LocalDate.now(BRASIL);
      AvisoFatura aviso;
      if (fatura.getDataVencimento().isBefore(hoje)) aviso = AvisoFatura.ATRASO;
      else if (fatura.getDataVencimento().isEqual(hoje)) aviso = AvisoFatura.DIA_VENCIMENTO;
      else if (fatura.getDataVencimento().isEqual(hoje.plusDays(1))) aviso = AvisoFatura.VESPERA;
      else aviso = AvisoFatura.FECHAMENTO;

      if (!enviarAvisoFatura(fatura, aviso)) {
         throw new BadRequestException("Mensagem não enviada, verifique os dados do cliente");
      }
   }

   private String montarMensagem(AvisoFatura aviso, String nome, String vencimento, String valor) {
      String abertura;
      String fechamento = "Evite a interrupção do serviço realizando o pagamento em dia.\n\n";
      switch (aviso) {
         case FECHAMENTO:
            abertura = "Sua fatura *AUTECH* foi fechada e já está disponível para pagamento.";
            break;
         case VESPERA:
            abertura = "Passando para lembrar que sua fatura *AUTECH* vence *amanhã*.";
            break;
         case DIA_VENCIMENTO:
            abertura = "Passando para lembrar que sua fatura *AUTECH* vence *hoje*.";
            break;
         default:
            abertura = "Identificamos que sua fatura *AUTECH* ainda está *em aberto*.";
            fechamento = "Evite a interrupção do serviço regularizando o pagamento. "
               + "Se já pagou, desconsidere esta mensagem.\n\n";
      }

      return "Olá, " + nome + "! 👋\n\n"
         + abertura + "\n\n"
         + "📅 Vencimento: *" + vencimento + "*\n"
         + "💰 Valor: *" + valor + "*\n\n"
         + "Para pagar via PIX, acesse o portal e informe seu CPF/CNPJ:\n"
         + "👉 " + portalUrl + "\n\n"
         + fechamento
         + "Dúvidas? Entre em contato conosco. 😊";
   }

   private String formatarValor(BigDecimal valor) {
      return "R$ " + valor.setScale(2, RoundingMode.HALF_UP).toPlainString().replace(".", ",");
   }

   private void enviar(String telefone, String mensagem, Logger errorLogger) {
      try {
         ResponseEntity<String> response = doSend(telefone, mensagem);
         log.info("WhatsApp enviado para {} - status {}", telefone, response.getStatusCode());
      } catch (Exception e) {
         errorLogger.error("Erro ao enviar WhatsApp para {}: {}", telefone, e.getMessage());
         throw new BadRequestException("Mensagem não enviada!");
      }
   }

   private ResponseEntity<String> doSend(String telefone, String mensagem) {
      String url = baseUrl + "/message/sendText/" + instance;

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("apikey", apiKey);

      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("number", telefone);
      payload.put("text", mensagem);

      return restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
   }

   private boolean isConfigurado() {
      return baseUrl != null && !baseUrl.isBlank()
         && instance != null && !instance.isBlank()
         && apiKey != null && !apiKey.isBlank();
   }

   private String normalizarTelefone(String telefone) {
      if (telefone == null || telefone.isBlank()) return null;

      String d = telefone.replaceAll("\\D", "");
      if (d.length() < 8) return null;

      // Já completo: 55 + DDD + 9 + 8 dígitos = 13
      if (d.length() == 13 && d.startsWith("55")) return d;

      // Tem código do país 55 mas formato irregular: remove e reprocessa
      if (d.startsWith("55") && d.length() > 11) d = d.substring(2);

      // DDD 81 + 9 + 8 dígitos = 11 (sem código do país)
      if (d.length() == 11 && d.startsWith("81")) return "55" + d;

      // DDD 81 + 8 dígitos = 10 (formato antigo sem o dígito 9)
      if (d.length() == 10 && d.startsWith("81")) return "5581" + "9" + d.substring(2);

      // 9 + 8 dígitos = 9 (sem DDD, com dígito 9)
      if (d.length() == 9 && d.startsWith("9")) return "5581" + d;

      // 8 dígitos: formato antigo sem DDD e sem o dígito 9
      if (d.length() == 8) return "5581" + "9" + d;

      // Fallback: pega os últimos 8 dígitos e monta o número completo
      return "5581" + "9" + d.substring(d.length() - 8);
   }

   private String resolverNomeCliente(Cliente cliente) {
      String nomeResp = cliente.getNomeResponsavel();
      if (nomeResp != null && !nomeResp.isBlank()) return nomeResp;
      return cliente.getNome() != null ? cliente.getNome() : "";
   }

   private String nomeClienteParaLog(Cliente cliente) {
      String nomeResp = cliente.getNomeResponsavel();
      if (nomeResp != null && !nomeResp.isBlank()) return nomeResp;
      return cliente.getNome() != null ? cliente.getNome() : "sem nome";
   }
}
