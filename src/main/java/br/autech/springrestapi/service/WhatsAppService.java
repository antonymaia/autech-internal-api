package br.autech.springrestapi.service;

import br.autech.springrestapi.dtos.ClienteDTO;
import br.autech.springrestapi.dtos.ClienteDadosCobrancaDto;
import br.autech.springrestapi.repository.ClienteRepository;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {
   private final ClienteRepository clienteRepository;

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
   @Value("${pix.chave}")
   private String chavePix;
   @Value("${pix.nome}")
   private String nomePix;


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
      LocalDate amanha = LocalDate.now(BRASIL).plusDays(1);

      String valor = "R$ " + "135.99".replace(".", ",");
      String vencimento = amanha.format(FORMATTER);

      String mensagem = String.format(
         "Olá, %s! 👋%n%n" +
            "Passando para lembrar que sua mensalidade *AUTECH* vence *amanhã*, %s.%n" +
            "Valor: *%s*%n%n" +
            "Chave Pix CNPJ%n" +
            "_%s_%n" +
            "_%s_%n%n" +
            "Evite a interrupção do serviço realizando o pagamento em dia.%n" +
            "Dúvidas? Entre em contato conosco. 😊",
         "", vencimento, valor, chavePix, nomePix);
      enviar(telefoneFormatado, mensagem, log);
   }

   // Enviado 1 dia antes do vencimento
   public void enviarAvisoCobranca(ClienteDTO cliente, LocalDate dataVencimento) {
      String contexto = "1 dia antes";
      if (cliente.getMensalidade() == null) {
         LOG_COBRANCA.warn("[{}] Cliente {} ({}) sem valorMensalidade. Mensagem nao enviada. Vencimento: {}, Telefone: {}",
            contexto, cliente.getCnpjCpf(), nomeParaLog(cliente),
            dataVencimento.format(FORMATTER), valorOuNd(cliente.getTelefone()));
         return;
      }
      String telefone = prepararTelefoneCliente(cliente, contexto, dataVencimento);
      if (telefone == null) return;

      String nome = resolverNome(cliente);
      String valor = "R$ " + cliente.getMensalidade().toPlainString().replace(".", ",");
      String vencimento = dataVencimento.format(FORMATTER);

      String mensagem = String.format(
         "Olá, %s! 👋%n%n" +
            "Passando para lembrar que sua mensalidade *AUTECH* vence *amanhã*, %s.%n" +
            "Valor: *%s*%n%n" +
            "Chave Pix CNPJ%n" +
            "_%s_%n" +
            "_%s_%n%n" +
            "Evite a interrupção do serviço realizando o pagamento em dia.%n" +
            "Dúvidas? Entre em contato conosco. 😊",
         nome, vencimento, valor, chavePix, nomePix);

      enviarCobranca(cliente, telefone, mensagem, contexto, dataVencimento);
   }

   // Enviado no dia do vencimento
   public void enviarAvisoCobrancaDia(ClienteDTO cliente, LocalDate dataVencimento) {
      String contexto = "dia do vencimento";
      if (cliente.getMensalidade() == null) {
         LOG_COBRANCA.warn("[{}] Cliente {} ({}) sem valorMensalidade. Mensagem nao enviada. Vencimento: {}, Telefone: {}",
            contexto, cliente.getCnpjCpf(), nomeParaLog(cliente),
            dataVencimento.format(FORMATTER), valorOuNd(cliente.getTelefone()));
         return;
      }
      String telefone = prepararTelefoneCliente(cliente, contexto, dataVencimento);
      if (telefone == null) return;

      String nome = resolverNome(cliente);
      String valor = "R$ " + cliente.getMensalidade().toPlainString().replace(".", ",");
      String vencimento = dataVencimento.format(FORMATTER);

      String mensagem = String.format(
         "Olá, %s! 👋%n%n" +
            "Passando para lembrar que sua mensalidade *AUTECH* vence *hoje*, %s.%n" +
            "Valor: *%s*%n%n" +
            "Chave Pix CNPJ%n" +
            "_%s_%n" +
            "_%s_%n%n" +
            "Evite a interrupção do serviço realizando o pagamento em dia.%n" +
            "Dúvidas? Entre em contato conosco. 😊",
         nome, vencimento, valor, chavePix, nomePix);

      enviarCobranca(cliente, telefone, mensagem, contexto, dataVencimento);
   }

   private String prepararTelefoneCliente(ClienteDTO cliente, String contexto, LocalDate dataVencimento) {
      String venc = dataVencimento.format(FORMATTER);
      String valor = cliente.getMensalidade() != null
         ? cliente.getMensalidade().toPlainString()
         : "n/d";

      if (!isConfigurado()) {
         LOG_COBRANCA.warn("[{}] WhatsApp Evolution API nao configurada. Cliente {} ({}) nao notificado. Vencimento: {}, Valor: {}",
            contexto, cliente.getCnpjCpf(), nomeParaLog(cliente), venc, valor);
         return null;
      }
      String telefone = cliente.getTelefone();
      if (telefone == null || telefone.isBlank()) {
         LOG_COBRANCA.warn("[{}] Cliente {} ({}) sem telefone cadastrado. Vencimento: {}, Valor: {}",
            contexto, cliente.getCnpjCpf(), nomeParaLog(cliente), venc, valor);
         return null;
      }
      String telefoneFormatado = normalizarTelefone(telefone);
      if (telefoneFormatado == null) {
         LOG_COBRANCA.warn("[{}] Telefone invalido para cliente {} ({}): '{}'. Vencimento: {}, Valor: {}",
            contexto, cliente.getCnpjCpf(), nomeParaLog(cliente), telefone, venc, valor);
         return null;
      }
      return telefoneFormatado;
   }

   private void enviarCobranca(ClienteDTO cliente, String telefone, String mensagem,
                               String contexto, LocalDate dataVencimento) {
      try {
         ResponseEntity<String> response = doSend(telefone, mensagem);
         log.info("WhatsApp [{}] enviado para {} ({}) tel {} - status {}",
            contexto, cliente.getCnpjCpf(), nomeParaLog(cliente),
            telefone, response.getStatusCode());
      } catch (Exception e) {
         String valor = cliente.getMensalidade() != null
            ? cliente.getMensalidade().toPlainString()
            : "n/d";
         LOG_COBRANCA.error("[{}] Falha ao enviar WhatsApp. Cliente: {} ({}), Telefone: {}, Vencimento: {}, Valor: {}, Erro: {}",
            contexto, cliente.getCnpjCpf(), nomeParaLog(cliente), telefone,
            dataVencimento.format(FORMATTER), valor, e.getMessage());
      }
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

   private String resolverNome(ClienteDTO cliente) {
      if (cliente.getNomeResponsavel() != null && cliente.getNomeResponsavel().isBlank()) {
         return cliente.getNomeResponsavel();
      }
      return cliente.getRazaoSocial();
   }

   private String nomeParaLog(ClienteDTO cliente) {
      String nomeResp = cliente.getNomeResponsavel();
      if (nomeResp != null && !nomeResp.isBlank()) return nomeResp;
      return cliente.getRazaoSocial() != null ? cliente.getRazaoSocial() : "sem nome";
   }

   private String valorOuNd(String valor) {
      return (valor == null || valor.isBlank()) ? "n/d" : valor;
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

   public void enviarMensagemCobranca(String cnpjCpf) {
      ClienteDadosCobrancaDto cliente = clienteRepository.buscarDadosCobranca(cnpjCpf);
      if (cliente.getValorMensalidade() == null || cliente.getValorMensalidade().compareTo(BigDecimal.ZERO) <= 0) {
         throw new BadRequestException("Mensagem não enviada, verifique os dados do cliente");
      }
      if (cliente.getDiaVencimento() == null || cliente.getDiaVencimento().isBlank()) {
         throw new BadRequestException("Mensagem não enviada, verique os dados do cliente");
      }
      if (cliente.getTelefone() == null || cliente.getTelefone().isBlank()) {
         throw new BadRequestException("Mensagem não enviada, verique os dados do cliente");
      }

      String nome = "";
      if (cliente.getNomeResponsavel() != null && !cliente.getNomeResponsavel().isBlank())
         nome = cliente.getNomeResponsavel();
      else nome = cliente.getRazaoSocial();

      String telefone = normalizarTelefone(cliente.getTelefone());

      LocalDate date = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
      String mes = date.getMonthValue() < 10 ? "0" + date.getMonthValue() : "" + date.getMonthValue();

      String mensagem = String.format(
         "Olá, %s! 👋%n%n" +
            "Empresa: _%s_%n" +
            "Passando para lembrar que sua mensalidade *AUTECH* venceu dia %s" + "/" + mes + "/" + date.getYear() + ".%n" +
            "Valor: *%s*%n%n" +
            "Chave Pix CNPJ%n" +
            "_%s_%n" +
            "_%s_%n%n" +
            "Evite a interrupção do serviço realizando o pagamento em dia.%n" +
            "Dúvidas? Entre em contato conosco. 😊",
         nome, cliente.getRazaoSocial(), cliente.getDiaVencimento(), cliente.getValorMensalidade(), chavePix, nomePix);

      enviar(telefone, mensagem, log);
   }
}
