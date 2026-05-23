package br.autech.springrestapi.service;

import br.autech.springrestapi.model.*;
import br.autech.springrestapi.model.enums.EstadoFatura;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaturaService {

    private final FaturaRepository faturaRepository;
    private final ClienteService clienteService;
    private final ResourceLoader resourceLoader;
    private final PagamentoFaturaRepository pagamentoFaturaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final JavaMailSender mailSender;
    private final TemplateBuilder templateBuilder;
    private final Environment environment;

    public Fatura buscarPorId(Long id){
        return faturaRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException("Fatura de id " + id + " não encontrada");
        });
    }
    public void gerarFaturas5dias(){
        List<Cliente> listaCliente = clienteService.buscarClientesPorDiaVencimentoa5dias();

        ZoneId brasilZone = ZoneId.of("America/Sao_Paulo");
        LocalDate dataVencimento = LocalDate.now(brasilZone).plusDays(5);

        listaCliente.forEach( cliente -> {
            Fatura fatura = new Fatura(
               null,
               EstadoFatura.ABERTA,
               cliente.getValorMensalidade(),
               dataVencimento,
               null,
               LocalDateTime.now(),
               cliente,
               null
            );

            fatura = faturaRepository.save(fatura);

            if(!cliente.getEmail().isBlank()){
                enviarEmailAvisoFaturaHTML(fatura);
            }
        });
    }


    public void enviarEmailAvisoFaturaHTML(Fatura fatura){
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
            htmlContent = htmlContent.replace("{{valorFatura}}", ""+fatura.getValor());
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

    public Fatura adicionarPagamento(Long faturaId, Pagamento pagamento) {
        Fatura fatura = buscarPorId(faturaId);

        if(fatura.getEstado().getCod() == 2){
            throw new BadRequestException("Fatura já paga");
        }

        BigDecimal valorTotalPagamentos = BigDecimal.valueOf(0);
        for(PagamentoFatura pf : fatura.getPagamentos()){
            valorTotalPagamentos = valorTotalPagamentos
                    .add(pf.getPagamento().getValorPagamento());
        }

        BigDecimal restantePagar = fatura.getValor().subtract(valorTotalPagamentos);
        if(pagamento.getValorPagamento().compareTo(restantePagar) > 0){
            throw new BadRequestException("Valor maior que o restante a pagar");
        }

        valorTotalPagamentos = valorTotalPagamentos.add(pagamento.getValorPagamento());

        pagamento.setCreatedAt(LocalDateTime.now());
        pagamento = pagamentoRepository.save(pagamento);

        PagamentoFaturaId pagamentoFaturaId = new PagamentoFaturaId(pagamento, fatura);
        PagamentoFatura pagamentoFatura = new PagamentoFatura(pagamentoFaturaId);

        fatura.getPagamentos().add(pagamentoFaturaRepository.save(pagamentoFatura));


        if(valorTotalPagamentos.equals(fatura.getValor())){
            fatura.setDataPagamento(LocalDateTime.now());
            fatura.setEstado(EstadoFatura.PAGA);
        }

        return faturaRepository.save(fatura);
    }

    public void atualizarEstadoFatura() {
        ZoneId brasilZone = ZoneId.of("America/Sao_Paulo");
        LocalDate dataAtual = LocalDate.now(brasilZone);

         List<Fatura> faturas = faturaRepository.findAllByDataVencimentoIsBeforeAndEstado(dataAtual, 1);

        faturas.forEach(fatura -> {
            fatura.setEstado(EstadoFatura.VENCIDA);
            faturaRepository.save(fatura);
        });
    }

    public void enviarAvisosCobranca1DiaAntes() {
        ZoneId brasilZone = ZoneId.of("America/Sao_Paulo");
        LocalDate amanha = LocalDate.now(brasilZone).plusDays(1);

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
