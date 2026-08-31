package br.autech.springrestapi.service;

import br.autech.springrestapi.dtos.ClienteDTO;
import br.autech.springrestapi.dtos.GerarFaturaResponse;
import br.autech.springrestapi.model.*;
import br.autech.springrestapi.model.enums.EstadoFatura;
import br.autech.springrestapi.model.enums.StatusAssinatura;
import br.autech.springrestapi.repository.AssinaturaRepository;
import br.autech.springrestapi.repository.ClienteRepository;
import br.autech.springrestapi.repository.FaturaRepository;
import br.autech.springrestapi.repository.PagamentoFaturaRepository;
import br.autech.springrestapi.repository.PagamentoRepository;
import br.autech.springrestapi.service.exception.BadRequestException;
import br.autech.springrestapi.service.exception.ObjectNotFoundException;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaturaService {

   private final FaturaRepository faturaRepository;
   private final ClienteService clienteService;
   private final ClienteRepository clienteRepository;
   private final AssinaturaRepository assinaturaRepository;
   private final ResourceLoader resourceLoader;
   private final PagamentoFaturaRepository pagamentoFaturaRepository;
   private final PagamentoRepository pagamentoRepository;
   private final JavaMailSender mailSender;
   private final TemplateBuilder templateBuilder;
   private final Environment environment;
   private final WhatsAppService whatsAppService;
   private final PlatformTransactionManager transactionManager;

   private static final ZoneId BRASIL = ZoneId.of("America/Sao_Paulo");
   private static final DateTimeFormatter FORMATTER_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

   public Fatura buscarPorId(Long id) {
      return faturaRepository.findById(id).orElseThrow(() -> {
         throw new ObjectNotFoundException("Fatura de id " + id + " não encontrada");
      });
   }

   public void gerarFaturas() {
      LocalDate dataVencimento = LocalDate.now().plusDays(5);
      int diaVencimentoBuscado = dataVencimento.getDayOfMonth();

      List<ClienteDTO> listaCliente = clienteService.buscarClientesAtivosPorDiaVencimento(diaVencimentoBuscado);

      log.info("[Gerar faturas] {} cliente(s) com dia_vencimento={}",
         listaCliente.size(), diaVencimentoBuscado);

      TransactionTemplate tx = new TransactionTemplate(transactionManager);
      int geradas = 0;
      int jaExistia = 0;
      int semAssinatura = 0;
      int assinaturaNaoAtiva = 0;
      int assinaturaSemProdutos = 0;

      for (ClienteDTO clienteDto : listaCliente) {
         try {
            ResultadoGeracao resultado = tx.execute(status -> processarCliente(clienteDto, dataVencimento));

            if (resultado == null) continue;
            switch (resultado.razao) {
               case GERADA:
                  geradas++;
                  whatsAppService.enviarAvisoFaturaGerada(resultado.cliente, resultado.fatura);
                  Thread.sleep(10000);
                  break;
               case JA_EXISTIA:            jaExistia++; break;
               case SEM_ASSINATURA:        semAssinatura++; break;
               case ASSINATURA_NAO_ATIVA:  assinaturaNaoAtiva++; break;
               case SEM_PRODUTOS:          assinaturaSemProdutos++; break;
            }
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Gerar faturas] Processo interrompido.", e);
            break;
         } catch (Exception e) {
            log.error("[Gerar faturas] Erro no cliente {}: {}", clienteDto.getCnpjCpf(), e.getMessage(), e);
         }
      }
      log.info("[Gerar faturas] Concluido. Geradas={}, ja existia={}, sem assinatura={}, assinatura nao ativa={}, sem produtos={}",
         geradas, jaExistia, semAssinatura, assinaturaNaoAtiva, assinaturaSemProdutos);
   }

   private ResultadoGeracao processarCliente(ClienteDTO clienteDto, LocalDate dataVencimento) {
      if (faturaRepository.existsByCliente_CnpjCpfAndDataVencimento(clienteDto.getCnpjCpf(), dataVencimento)) {
         return new ResultadoGeracao(Razao.JA_EXISTIA, null, null);
      }

      Cliente cliente = clienteService.findByCnpjCpf(clienteDto.getCnpjCpf());
      Assinatura assinatura = cliente.getAssinatura();
      if (assinatura == null) {
         log.warn("[Gerar faturas] Cliente {} sem assinatura. Fatura nao gerada.", cliente.getCnpjCpf());
         return new ResultadoGeracao(Razao.SEM_ASSINATURA, null, null);
      }
      if (assinatura.getStatus() != StatusAssinatura.ATIVA) {
         return new ResultadoGeracao(Razao.ASSINATURA_NAO_ATIVA, null, null);
      }
      if (assinatura.getProdutos() == null || assinatura.getProdutos().isEmpty()) {
         log.warn("[Gerar faturas] Cliente {} tem assinatura ATIVA sem produtos. Fatura nao gerada.",
            cliente.getCnpjCpf());
         return new ResultadoGeracao(Razao.SEM_PRODUTOS, null, null);
      }

      Fatura fatura = montarFaturaComItens(cliente, assinatura, dataVencimento);
      Fatura salva = faturaRepository.save(fatura);
      return new ResultadoGeracao(Razao.GERADA, cliente, salva);
   }

   private enum Razao {
      GERADA, JA_EXISTIA, SEM_ASSINATURA, ASSINATURA_NAO_ATIVA, SEM_PRODUTOS
   }

   private static class ResultadoGeracao {
      final Razao razao;
      final Cliente cliente;
      final Fatura fatura;

      ResultadoGeracao(Razao razao, Cliente cliente, Fatura fatura) {
         this.razao = razao;
         this.cliente = cliente;
         this.fatura = fatura;
      }
   }

   private Fatura montarFaturaComItens(Cliente cliente, Assinatura assinatura, LocalDate dataVencimento) {
      Fatura fatura = new Fatura();
      fatura.setCliente(cliente);
      fatura.setDataVencimento(dataVencimento);
      fatura.setEstado(EstadoFatura.ABERTA);
      fatura.setCreatedAt(LocalDateTime.now());

      List<FaturaItem> itens = new ArrayList<>();
      for (AssinaturaProduto ap : assinatura.getProdutos()) {
         Produto produto = ap.getProduto();
         int qtd = ap.getQuantidade() != null ? ap.getQuantidade() : 1;
         BigDecimal valorUnitario = ap.getValor() != null ? ap.getValor() : BigDecimal.ZERO;
         BigDecimal subtotal = valorUnitario.multiply(BigDecimal.valueOf(qtd));

         FaturaItem item = new FaturaItem();
         item.setFatura(fatura);
         item.setProduto(produto);
         item.setDescricao(produto.getNome());
         item.setValorUnitario(valorUnitario);
         item.setQuantidade(qtd);
         item.setSubtotal(subtotal);
         itens.add(item);
      }

      BigDecimal valorTotal = itens.stream()
         .map(FaturaItem::getSubtotal)
         .reduce(BigDecimal.ZERO, BigDecimal::add);
      fatura.setValor(valorTotal);
      fatura.setItens(itens);
      return fatura;
   }

   /**
    * Geração manual da fatura de UM cliente para o mes atual.
    * Usada quando o cron não rodou (ex.: deploy no meio do dia) e é preciso
    * criar a fatura sob demanda. Respeita todos os critérios de elegibilidade
    * do cron. Se ja existe fatura pro vencimento calculado, retorna aviso sem
    * duplicar.
    */
   @Transactional
   public GerarFaturaResponse gerarFaturaManual(String cnpjCpf) {
      Cliente cliente = clienteService.findByCnpjCpf(cnpjCpf);

      if (!"S".equalsIgnoreCase(cliente.getAtivo())) {
         return new GerarFaturaResponse(false,
            "Cliente esta inativo. Fatura nao gerada.", null);
      }

      Assinatura assinatura = cliente.getAssinatura();
      if (assinatura == null) {
         return new GerarFaturaResponse(false,
            "Cliente nao possui assinatura. Fatura nao gerada.", null);
      }
      if (assinatura.getStatus() != StatusAssinatura.ATIVA) {
         return new GerarFaturaResponse(false,
            "Assinatura nao esta ATIVA (atual: " + assinatura.getStatus() + "). Fatura nao gerada.",
            null);
      }
      if (assinatura.getProdutos() == null || assinatura.getProdutos().isEmpty()) {
         return new GerarFaturaResponse(false,
            "Assinatura nao possui produtos. Fatura nao gerada.", null);
      }

      int dia;
      try {
         dia = Integer.parseInt(cliente.getDiaVencimento());
      } catch (NumberFormatException e) {
         return new GerarFaturaResponse(false,
            "diaVencimento invalido no cadastro do cliente: '" + cliente.getDiaVencimento() + "'.",
            null);
      }

      LocalDate hoje = LocalDate.now(BRASIL);
      int diaAjustado = Math.min(dia, hoje.lengthOfMonth());
      LocalDate dataVencimento = hoje.withDayOfMonth(diaAjustado);

      if (faturaRepository.existsByCliente_CnpjCpfAndDataVencimento(cnpjCpf, dataVencimento)) {
         Fatura existente = faturaRepository.findAllByCliente(cliente).stream()
            .filter(f -> dataVencimento.equals(f.getDataVencimento()))
            .findFirst()
            .orElse(null);
         return new GerarFaturaResponse(false,
            "Ja existe fatura para o vencimento " + dataVencimento.format(FORMATTER_BR) + ".",
            existente);
      }

      Fatura fatura = montarFaturaComItens(cliente, assinatura, dataVencimento);
      fatura = faturaRepository.save(fatura);
      whatsAppService.enviarAvisoFaturaGerada(cliente, fatura);

      return new GerarFaturaResponse(true,
         "Fatura gerada com sucesso para vencimento " + dataVencimento.format(FORMATTER_BR) + ".",
         fatura);
   }

   @Transactional
   public void bloquearInadimplentes() {
      LocalDate limite = LocalDate.now(BRASIL).minusDays(7);
      List<Fatura> vencidas = faturaRepository.findAllByDataVencimentoIsBeforeAndEstado(limite, EstadoFatura.VENCIDA.getCod());
      log.info("[Bloqueio D+7] {} fatura(s) vencidas ha mais de 7 dias", vencidas.size());

      Set<String> processados = new HashSet<>();
      int bloqueados = 0;
      for (Fatura fatura : vencidas) {
         Cliente cliente = fatura.getCliente();
         if (cliente == null || processados.contains(cliente.getCnpjCpf())) continue;
         processados.add(cliente.getCnpjCpf());

         if ("S".equalsIgnoreCase(cliente.getBloqueado())) continue;

         cliente.setBloqueado("S");
         clienteRepository.save(cliente);

         Assinatura assinatura = cliente.getAssinatura();
         if (assinatura != null && assinatura.getStatus() != StatusAssinatura.SUSPENSA) {
            assinatura.setStatus(StatusAssinatura.SUSPENSA);
            assinaturaRepository.save(assinatura);
         }

         log.warn("[Bloqueio D+7] Cliente {} ({}) bloqueado. Fatura {}, vencimento {}, valor {}",
            cliente.getCnpjCpf(), cliente.getNome(), fatura.getId(),
            fatura.getDataVencimento(), fatura.getValor());
         bloqueados++;
      }
      log.info("[Bloqueio D+7] Concluido. Bloqueados: {}", bloqueados);
   }

   public void enviarEmailAvisoFaturaHTML(Fatura fatura) {
      final String username = "autechcomercial@gmail.com";
      final String password = "fpskofysocsbvfrv";

      Properties props = new Properties();
      props.put("mail.smtp.auth", "true");
      props.put("mail.smtp.starttls.enable", "true");
      props.put("mail.smtp.host", "smtp.gmail.com");
      props.put("mail.smtp.port", "587");

      Session session = Session.getInstance(props, new Authenticator() {
         protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
         }
      });

      try {
         Message message = new MimeMessage(session);
         message.setFrom(new InternetAddress(username));
         message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(fatura.getCliente().getEmail()));
         message.setSubject("Autech - Sua licença expira em 5 dias");

         String htmlContent = loadEmailTemplate("email-licenca-vencida.html");
         htmlContent = htmlContent.replace("{{valorFatura}}", "" + fatura.getValor());
         message.setContent(htmlContent, "text/html; charset=utf-8");

         Transport.send(message);
         System.out.println("E-mail enviado com sucesso!");
      } catch (MessagingException | IOException e) {
         e.printStackTrace();
      }
   }

   public String loadEmailTemplate(String templatePath) throws IOException {
      Resource resource = resourceLoader.getResource("classpath:templates/" + templatePath);
      StringBuilder contentBuilder = new StringBuilder();
      try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
         String line;
         while ((line = br.readLine()) != null) {
            contentBuilder.append(line);
         }
      }
      return contentBuilder.toString();
   }


   public List<Fatura> buscarFaturasCliente(String cnpjCpf) {
      Cliente cliente = clienteService.findByCnpjCpf(cnpjCpf);
      return faturaRepository.findAllByCliente(cliente);
   }

   @Transactional
   public Fatura adicionarPagamento(Long faturaId, Pagamento pagamento) {
      Fatura fatura = buscarPorId(faturaId);

      if (fatura.getEstado() == EstadoFatura.PAGA) {
         throw new BadRequestException("Fatura já paga");
      }
      if (!fatura.getPagamentos().isEmpty()) {
         throw new BadRequestException("Fatura já possui pagamento registrado");
      }
      if (pagamento.getValorPagamento() == null) {
         throw new BadRequestException("valorPagamento obrigatorio");
      }
      if (pagamento.getValorPagamento().compareTo(fatura.getValor()) != 0) {
         throw new BadRequestException(
            "Pagamento deve ser integral. Valor esperado: " + fatura.getValor().toPlainString());
      }

      pagamento.setCreatedAt(LocalDateTime.now());
      pagamento = pagamentoRepository.save(pagamento);

      PagamentoFaturaId pagamentoFaturaId = new PagamentoFaturaId(pagamento, fatura);
      PagamentoFatura pagamentoFatura = new PagamentoFatura(pagamentoFaturaId);
      fatura.getPagamentos().add(pagamentoFaturaRepository.save(pagamentoFatura));

      fatura.setDataPagamento(LocalDateTime.now());
      fatura.setEstado(EstadoFatura.PAGA);

      return faturaRepository.save(fatura);
   }

   public void atualizarEstadoFatura() {
      LocalDate dataAtual = LocalDate.now(BRASIL);

      List<Fatura> faturas = faturaRepository.findAllByDataVencimentoIsBeforeAndEstado(dataAtual, 1);

      faturas.forEach(fatura -> {
         fatura.setEstado(EstadoFatura.VENCIDA);
         faturaRepository.save(fatura);
      });
   }

   public void enviarAvisosCobranca1DiaAntes() {
      LocalDate amanha = LocalDate.now(BRASIL).plusDays(1);

      List<Fatura> faturas = faturaRepository.findAllByDataVencimentoAndEstado(amanha, 1);

      faturas.forEach(fatura -> {
         Cliente cliente = fatura.getCliente();
         if (cliente.getEmail() != null && !cliente.getEmail().isBlank()) {
            enviarEmailAvisoCobranca1Dia(fatura);
         }
      });
   }

   private void enviarEmailAvisoCobranca1Dia(Fatura fatura) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
      String nomeResponsavel = fatura.getCliente().getNomeResponsavel();
      String nomeCliente = (nomeResponsavel != null && !nomeResponsavel.isBlank())
         ? nomeResponsavel
         : fatura.getCliente().getNome();

      Map<String, Object> variables = new HashMap<>();
      variables.put("nomeCliente", nomeCliente);
      variables.put("dataVencimento", fatura.getDataVencimento().format(formatter));
      variables.put("valorFatura", fatura.getValor());

      MimeMessagePreparator messagePreparator = mimeMessage -> {
         MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
         messageHelper.setFrom(Objects.requireNonNull(environment.getProperty("spring.mail.username")));
         messageHelper.setTo(fatura.getCliente().getEmail());
         messageHelper.setSubject("Autech - Aviso de cobrança");
         messageHelper.setText(templateBuilder.build("email-aviso-cobranca", variables), true);
      };

      try {
         mailSender.send(messagePreparator);
      } catch (MailException e) {
         log.error("Erro ao enviar email de cobrança para {}: {}", fatura.getCliente().getEmail(), e.getMessage());
      }
   }
}
