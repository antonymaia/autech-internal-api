package br.autech.springrestapi.service;

import br.autech.springrestapi.dtos.ClienteDTO;
import br.autech.springrestapi.dtos.GerarFaturaResponse;
import br.autech.springrestapi.model.*;
import br.autech.springrestapi.model.enums.EstadoFatura;
import br.autech.springrestapi.model.enums.StatusAssinatura;
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
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaturaService {

   private final FaturaRepository faturaRepository;
   private final ClienteService clienteService;
   private final ClienteRepository clienteRepository;
   private final ResourceLoader resourceLoader;
   private final PagamentoFaturaRepository pagamentoFaturaRepository;
   private final PagamentoRepository pagamentoRepository;
   private final JavaMailSender mailSender;
   private final TemplateBuilder templateBuilder;
   private final Environment environment;
   private final PlatformTransactionManager transactionManager;

   public static final int DIAS_FECHAMENTO = 3;
   public static final int DIAS_BLOQUEIO = 10;
   private static final List<Integer> ESTADOS_PENDENTES =
      List.of(EstadoFatura.FECHADA.getCod(), EstadoFatura.VENCIDA.getCod());
   private static final ZoneId BRASIL = ZoneId.of("America/Sao_Paulo");
   private static final DateTimeFormatter FORMATTER_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

   public Fatura buscarPorId(Long id) {
      return faturaRepository.findById(id).orElseThrow(() -> {
         throw new ObjectNotFoundException("Fatura de id " + id + " não encontrada");
      });
   }

   /**
    * Fecha as faturas que vencem daqui a DIAS_FECHAMENTO dias e, para cada fatura fechada,
    * ja cria a fatura do proximo mes com os itens da assinatura atual (ABERTA).
    *
    * Tambem fecha faturas ABERTA com vencimento entre hoje e a data alvo que ficaram
    * para tras (ex.: cron nao rodou). Se o cliente ainda nao tem fatura para a data alvo
    * (cliente novo), ela e criada e fechada na hora.
    *
    * Retorna as faturas fechadas nesta execucao, para o envio do aviso de fechamento.
    */
   public List<Fatura> fecharFaturas() {
      LocalDate hoje = LocalDate.now(BRASIL);
      LocalDate dataAlvo = hoje.plusDays(DIAS_FECHAMENTO);

      Set<String> clientes = new LinkedHashSet<>();
      buscarClientesAtivosComVencimentoEm(dataAlvo).forEach(c -> clientes.add(c.getCnpjCpf()));
      faturaRepository.findAllByEstadoAndDataVencimentoBetween(EstadoFatura.ABERTA.getCod(), hoje, dataAlvo)
         .forEach(f -> clientes.add(f.getCliente().getCnpjCpf()));

      log.info("[Fechar faturas] {} cliente(s) com vencimento ate {}", clientes.size(), dataAlvo.format(FORMATTER_BR));

      TransactionTemplate tx = new TransactionTemplate(transactionManager);
      List<Fatura> fechadas = new ArrayList<>();
      int proximasGeradas = 0;
      Map<Razao, Integer> ignorados = new EnumMap<>(Razao.class);

      for (String cnpjCpf : clientes) {
         try {
            ResultadoFechamento resultado = tx.execute(status -> processarFechamento(cnpjCpf, hoje, dataAlvo));
            if (resultado == null) continue;
            if (resultado.razao != null) {
               ignorados.merge(resultado.razao, 1, Integer::sum);
               continue;
            }
            fechadas.addAll(resultado.fechadas);
            proximasGeradas += resultado.proximasGeradas;
         } catch (Exception e) {
            log.error("[Fechar faturas] Erro no cliente {}: {}", cnpjCpf, e.getMessage(), e);
         }
      }

      log.info("[Fechar faturas] Concluido. Fechadas={}, proximas geradas={}, ignorados={}",
         fechadas.size(), proximasGeradas, ignorados);
      return fechadas;
   }

   private ResultadoFechamento processarFechamento(String cnpjCpf, LocalDate hoje, LocalDate dataAlvo) {
      Cliente cliente = clienteService.findByCnpjCpf(cnpjCpf);
      Razao inelegivel = verificarElegibilidade(cliente);
      if (inelegivel != null) {
         log.warn("[Fechar faturas] Cliente {} nao elegivel ({}). Nenhuma fatura fechada.", cnpjCpf, inelegivel);
         return ResultadoFechamento.ignorado(inelegivel);
      }

      Assinatura assinatura = cliente.getAssinatura();
      Integer dia = parseDiaVencimento(cliente.getDiaVencimento());

      List<Fatura> aFechar = new ArrayList<>(faturaRepository.findAllByCliente_CnpjCpfAndEstadoAndDataVencimentoBetween(
         cnpjCpf, EstadoFatura.ABERTA.getCod(), hoje, dataAlvo));

      if (dia != null && dataAlvo.equals(vencimentoNoMes(YearMonth.from(dataAlvo), dia))
         && !faturaRepository.existsByCliente_CnpjCpfAndDataVencimento(cnpjCpf, dataAlvo)) {
         aFechar.add(montarFaturaComItens(cliente, assinatura, dataAlvo));
      }

      if (aFechar.isEmpty()) return ResultadoFechamento.ignorado(Razao.NADA_A_FECHAR);

      List<Fatura> fechadas = new ArrayList<>();
      int proximasGeradas = 0;
      for (Fatura fatura : aFechar) {
         fatura.setEstado(EstadoFatura.FECHADA);
         fechadas.add(faturaRepository.save(fatura));

         if (dia == null) {
            log.warn("[Fechar faturas] Cliente {} com diaVencimento invalido ('{}'). Proxima fatura nao gerada.",
               cnpjCpf, cliente.getDiaVencimento());
            continue;
         }
         LocalDate proximoVencimento = vencimentoNoMes(YearMonth.from(fatura.getDataVencimento()).plusMonths(1), dia);
         if (!faturaRepository.existsByCliente_CnpjCpfAndDataVencimento(cnpjCpf, proximoVencimento)) {
            faturaRepository.save(montarFaturaComItens(cliente, assinatura, proximoVencimento));
            proximasGeradas++;
         }
      }
      return new ResultadoFechamento(null, fechadas, proximasGeradas);
   }

   private Razao verificarElegibilidade(Cliente cliente) {
      if (!"S".equalsIgnoreCase(cliente.getAtivo())) return Razao.CLIENTE_INATIVO;
      Assinatura assinatura = cliente.getAssinatura();
      if (assinatura == null) return Razao.SEM_ASSINATURA;
      if (assinatura.getStatus() != StatusAssinatura.ATIVA) return Razao.ASSINATURA_NAO_ATIVA;
      if (assinatura.getProdutos() == null || assinatura.getProdutos().isEmpty()) return Razao.SEM_PRODUTOS;
      return null;
   }

   /**
    * Clientes ativos cujo vencimento cai na data informada. No ultimo dia do mes
    * inclui tambem quem vence em dias que o mes nao tem (ex.: dia 31 em novembro vence dia 30).
    */
   private List<ClienteDTO> buscarClientesAtivosComVencimentoEm(LocalDate data) {
      List<ClienteDTO> clientes = new ArrayList<>(clienteService.buscarClientesAtivosPorDiaVencimento(data.getDayOfMonth()));
      if (data.getDayOfMonth() == data.lengthOfMonth()) {
         for (int dia = data.getDayOfMonth() + 1; dia <= 31; dia++) {
            clientes.addAll(clienteService.buscarClientesAtivosPorDiaVencimento(dia));
         }
      }
      return clientes;
   }

   private LocalDate vencimentoNoMes(YearMonth mes, int dia) {
      return mes.atDay(Math.min(dia, mes.lengthOfMonth()));
   }

   private Integer parseDiaVencimento(String diaVencimento) {
      try {
         int dia = Integer.parseInt(diaVencimento.trim());
         return dia >= 1 && dia <= 31 ? dia : null;
      } catch (NullPointerException | NumberFormatException e) {
         return null;
      }
   }

   /**
    * Faturas fechadas ou vencidas, ainda nao pagas, com vencimento na data informada.
    */
   public List<Fatura> buscarPendentesComVencimentoEm(LocalDate dataVencimento) {
      return faturaRepository.findAllByDataVencimentoAndEstadoIn(dataVencimento, ESTADOS_PENDENTES);
   }

   /**
    * Rele a fatura do banco antes de um envio: devolve vazio se ela ja foi paga,
    * nao esta mais pendente ou o cliente foi inativado.
    */
   @Transactional(readOnly = true)
   public Optional<Fatura> buscarPendenteParaAviso(Long faturaId) {
      return faturaRepository.findById(faturaId)
         .filter(f -> f.getEstado() != null && ESTADOS_PENDENTES.contains(f.getEstado().getCod()))
         .filter(f -> f.getCliente() != null && "S".equalsIgnoreCase(f.getCliente().getAtivo()));
   }

   private enum Razao {
      NADA_A_FECHAR, CLIENTE_INATIVO, SEM_ASSINATURA, ASSINATURA_NAO_ATIVA, SEM_PRODUTOS
   }

   private static class ResultadoFechamento {
      final Razao razao;
      final List<Fatura> fechadas;
      final int proximasGeradas;

      ResultadoFechamento(Razao razao, List<Fatura> fechadas, int proximasGeradas) {
         this.razao = razao;
         this.fechadas = fechadas;
         this.proximasGeradas = proximasGeradas;
      }

      static ResultadoFechamento ignorado(Razao razao) {
         return new ResultadoFechamento(razao, List.of(), 0);
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

      return new GerarFaturaResponse(true,
         "Fatura gerada com sucesso para vencimento " + dataVencimento.format(FORMATTER_BR) + ".",
         fatura);
   }

   /**
    * Bloqueia clientes com fatura nao paga ha DIAS_BLOQUEIO dias ou mais do vencimento
    * (vencimento dia 10 -> bloqueio dia 20). A assinatura nao e suspensa para que o
    * fechamento continue gerando as proximas faturas.
    */
   @Transactional
   public void bloquearInadimplentes() {
      LocalDate limite = LocalDate.now(BRASIL).minusDays(DIAS_BLOQUEIO);
      List<Fatura> atrasadas = faturaRepository.findAllByDataVencimentoLessThanEqualAndEstadoIn(limite, ESTADOS_PENDENTES);
      log.info("[Bloqueio D+{}] {} fatura(s) nao paga(s) com vencimento ate {}",
         DIAS_BLOQUEIO, atrasadas.size(), limite.format(FORMATTER_BR));

      Set<String> processados = new HashSet<>();
      int bloqueados = 0;
      for (Fatura fatura : atrasadas) {
         Cliente cliente = fatura.getCliente();
         if (cliente == null || !processados.add(cliente.getCnpjCpf())) continue;
         if ("S".equalsIgnoreCase(cliente.getBloqueado())) continue;

         cliente.setBloqueado("S");
         clienteRepository.save(cliente);

         log.warn("[Bloqueio D+{}] Cliente {} ({}) bloqueado. Fatura {}, vencimento {}, valor {}",
            DIAS_BLOQUEIO, cliente.getCnpjCpf(), cliente.getNome(), fatura.getId(),
            fatura.getDataVencimento(), fatura.getValor());
         bloqueados++;
      }
      log.info("[Bloqueio D+{}] Concluido. Bloqueados: {}", DIAS_BLOQUEIO, bloqueados);
   }

   /**
    * Chamado apos registrar um pagamento: desbloqueia o cliente se ele nao tem
    * mais nenhuma fatura pendente que justifique o bloqueio.
    */
   public void desbloquearSeQuitado(Cliente cliente) {
      if (cliente == null || !"S".equalsIgnoreCase(cliente.getBloqueado())) return;

      LocalDate limite = LocalDate.now(BRASIL).minusDays(DIAS_BLOQUEIO);
      if (faturaRepository.existsByCliente_CnpjCpfAndDataVencimentoLessThanEqualAndEstadoIn(
         cliente.getCnpjCpf(), limite, ESTADOS_PENDENTES)) {
         return;
      }

      cliente.setBloqueado("N");
      clienteRepository.save(cliente);
      log.info("[Desbloqueio] Cliente {} ({}) desbloqueado apos pagamento.", cliente.getCnpjCpf(), cliente.getNome());
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
      fatura = faturaRepository.save(fatura);

      desbloquearSeQuitado(fatura.getCliente());
      return fatura;
   }

   public void atualizarEstadoFatura() {
      LocalDate dataAtual = LocalDate.now(BRASIL);

      List<Fatura> faturas = faturaRepository.findAllByDataVencimentoIsBeforeAndEstado(dataAtual, EstadoFatura.FECHADA.getCod());

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
