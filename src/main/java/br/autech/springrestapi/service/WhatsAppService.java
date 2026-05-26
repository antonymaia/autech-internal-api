package br.autech.springrestapi.service;

import br.autech.springrestapi.model.Cliente;
import br.autech.springrestapi.repository.ClienteRepository;
import br.autech.springrestapi.service.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {

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


    private final ClienteRepository clienteRepository;

    public void enviarMensagemTeste(String telefone, String mensagem) {
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

        mensagem = String.format(
           "Olá, %s! 👋%n%n" +
              "Passando para lembrar que sua mensalidade *AUTECH* vence *amanhã*, %s.%n" +
              "Valor: *%s*%n%n" +
              "Chave Pix CNPJ%n" +
              "_%s_%n" +
              "_%s_%n%n" +
              "Evite a interrupção do serviço realizando o pagamento em dia.%n" +
              "Dúvidas? Entre em contato conosco. 😊",
           "Antony Maia", vencimento, valor, chavePix, nomePix);
        enviar(telefoneFormatado, mensagem);
    }

    // Enviado 1 dia antes do vencimento
    public void enviarAvisoCobranca(Cliente cliente, LocalDate dataVencimento) {
        if (cliente.getValorMensalidade() == null) {
            log.warn("Cliente {} sem valorMensalidade. WhatsApp não enviado.", cliente.getCnpjCpf());
            return;
        }
        String telefone = prepararTelefoneCliente(cliente);
        if (telefone == null) return;

        String nome = resolverNome(cliente);
        String valor = "R$ " + cliente.getValorMensalidade().toPlainString().replace(".", ",");
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
           "Antony Maia", vencimento, valor, chavePix, nomePix);

        enviar(telefone, mensagem);
    }

    // Enviado no dia do vencimento
    public void enviarAvisoCobrancaDia(Cliente cliente, LocalDate dataVencimento) {
        if (cliente.getValorMensalidade() == null) {
            log.warn("Cliente {} sem valorMensalidade. WhatsApp não enviado.", cliente.getCnpjCpf());
            return;
        }
        String telefone = prepararTelefoneCliente(cliente);
        if (telefone == null) return;

        String nome = resolverNome(cliente);
        String valor = "R$ " + cliente.getValorMensalidade().toPlainString().replace(".", ",");
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
           "Antony Maia", vencimento, valor, chavePix, nomePix);

        enviar(telefone, mensagem);
    }

    private String prepararTelefoneCliente(Cliente cliente) {
        if (!isConfigurado()) {
            log.warn("WhatsApp Evolution API não configurada. Mensagem não enviada.");
            return null;
        }
        String telefone = cliente.getTelefone();
        if (telefone == null || telefone.isBlank()) {
            log.warn("Cliente {} sem telefone cadastrado.", cliente.getCnpjCpf());
            return null;
        }
        String telefoneFormatado = normalizarTelefone(telefone);
        if (telefoneFormatado == null) {
            log.warn("Telefone inválido para cliente {}: '{}'", cliente.getCnpjCpf(), telefone);
            return null;
        }
        if (!telefoneFormatado.equals(telefone)) {
            cliente.setTelefone(telefoneFormatado);
            clienteRepository.save(cliente);
            log.info("Telefone do cliente {} atualizado: '{}' -> '{}'", cliente.getCnpjCpf(), telefone, telefoneFormatado);
        }
        return telefoneFormatado;
    }

    private void enviar(String telefone, String mensagem) {
        String url = baseUrl + "/message/sendText/" + instance;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", apiKey);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("number", telefone);
        payload.put("text", mensagem);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(payload, headers), String.class);
            log.info("WhatsApp enviado para {} - status {}", telefone, response.getStatusCode());
        } catch (Exception e) {
            log.error("Erro ao enviar WhatsApp para {}: {}", telefone, e.getMessage());
        }
    }

    private String resolverNome(Cliente cliente) {
        String nomeResponsavel = cliente.getNomeResponsavel();
        return (nomeResponsavel != null && !nomeResponsavel.isBlank())
                ? nomeResponsavel
                : cliente.getNome();
    }

    private boolean isConfigurado() {
        return baseUrl != null && !baseUrl.isBlank()
                && instance != null && !instance.isBlank()
                && apiKey != null && !apiKey.isBlank();
    }

    /**
     * Normaliza o telefone para o formato E.164 brasileiro (Pernambuco, celular).
     * Resultado: 55 + 81 + 9 + 8 dígitos = 13 dígitos.
     * Retorna null se houver menos de 8 dígitos numéricos.
     */
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
}
