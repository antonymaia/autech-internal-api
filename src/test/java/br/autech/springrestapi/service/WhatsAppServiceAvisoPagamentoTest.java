package br.autech.springrestapi.service;

import br.autech.springrestapi.repository.FaturaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(WhatsAppServiceAvisoPagamentoTest.Config.class)
@TestPropertySource(properties = {
    "whatsapp.evolution.base-url=http://evolution.test",
    "whatsapp.evolution.instance.name=instancia",
    "whatsapp.evolution.api-key=chave",
    "whatsapp.aviso-pagamento.telefones=" + WhatsAppServiceAvisoPagamentoTest.TELEFONES
})
class WhatsAppServiceAvisoPagamentoTest {

    static final String TELEFONE_1 = "5511988887777";
    static final String TELEFONE_2 = "5511966665555";
    static final String TELEFONES = TELEFONE_1 + "," + TELEFONE_2;
    static final String TELEFONE_CLIENTE = "5581977776666";

    @Configuration
    @EnableAsync
    @EnableTransactionManagement
    static class Config {
        @Bean
        WhatsAppService whatsAppService() {
            return new WhatsAppService(Mockito.mock(FaturaRepository.class));
        }

        @Bean
        TaskExecutor taskExecutor() {
            return new SyncTaskExecutor();
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() {
                    return new Object();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) {
                }
            };
        }
    }

    @Autowired
    WhatsAppService whatsAppService;
    @Autowired
    ApplicationEventPublisher eventPublisher;
    @Autowired
    PlatformTransactionManager transactionManager;

    private final List<MockClientHttpRequest> enviados = new ArrayList<>();
    private RestTemplate restTemplate;
    private ClientHttpRequestFactory factoryOriginal;

    @BeforeEach
    void setUp() {
        restTemplate = (RestTemplate) ReflectionTestUtils.getField(WhatsAppService.class, "restTemplate");
        factoryOriginal = restTemplate.getRequestFactory();
        restTemplate.setRequestFactory((uri, method) -> {
            MockClientHttpRequest request = new MockClientHttpRequest(method, uri);
            request.setResponse(new MockClientHttpResponse(new byte[0], HttpStatus.OK));
            enviados.add(request);
            return request;
        });
    }

    @AfterEach
    void tearDown() {
        restTemplate.setRequestFactory(factoryOriginal);
        definirTelefones(TELEFONES);
    }

    private void definirTelefones(String telefones) {
        Object alvo = AopTestUtils.getTargetObject(whatsAppService);
        ReflectionTestUtils.setField(alvo, "telefonesAvisoPagamento", telefones);
    }

    private PagamentoConfirmadoEvent.PagamentoConfirmadoEventBuilder evento() {
        return PagamentoConfirmadoEvent.builder()
            .faturaId(42L)
            .cnpjCpf("12345678000199")
            .nomeCliente("Mercado Bom Preco")
            .nomeResponsavel("Maria")
            .telefoneCliente(TELEFONE_CLIENTE)
            .valor(new BigDecimal("135.9"))
            .dataPagamento(LocalDateTime.of(2026, 11, 2, 14, 35));
    }

    private String corpoPara(String telefone) {
        List<String> corpos = enviados.stream()
            .map(MockClientHttpRequest::getBodyAsString)
            .filter(corpo -> corpo.contains("\"number\":\"" + telefone + "\""))
            .collect(Collectors.toList());
        assertEquals(1, corpos.size(), "mensagens para " + telefone);
        return corpos.get(0);
    }

    @Test
    void depoisDoCommitAvisaOsDoisNumerosEOCliente() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(evento().build());
            assertTrue(enviados.isEmpty(), "nao pode enviar antes do commit");
        });

        assertEquals(3, enviados.size());
        assertEquals("http://evolution.test/message/sendText/instancia", enviados.get(0).getURI().toString());

        for (String telefone : List.of(TELEFONE_1, TELEFONE_2)) {
            String interno = corpoPara(telefone);
            assertTrue(interno.contains("Pagamento PIX recebido"), interno);
            assertTrue(interno.contains("Mercado Bom Preco"), interno);
            assertTrue(interno.contains("12345678000199"), interno);
            assertTrue(interno.contains("*42*"), interno);
            assertTrue(interno.contains("R$ 135,90"), interno);
            assertTrue(interno.contains("02/11/2026 14:35"), interno);
        }

        String cliente = corpoPara(TELEFONE_CLIENTE);
        assertTrue(cliente.contains("Maria"), cliente);
        assertTrue(cliente.contains("Recebemos o pagamento"), cliente);
        assertTrue(cliente.contains("R$ 135,90"), cliente);
        assertTrue(cliente.contains("Caso seu sistema esteja bloqueado"), cliente);
    }

    @Test
    void clienteAindaBloqueadoNaoRecebePromessaDeDesbloqueioImediato() {
        whatsAppService.enviarAvisoPagamento(evento().clienteBloqueado(true).build());

        String cliente = corpoPara(TELEFONE_CLIENTE);
        assertTrue(cliente.contains("continua bloqueado"), cliente);
        assertFalse(cliente.contains("Caso seu sistema esteja bloqueado"), cliente);
    }

    @Test
    void clienteSemResponsavelESaudadoPeloNome() {
        whatsAppService.enviarAvisoPagamento(evento().nomeResponsavel(" ").build());

        assertTrue(corpoPara(TELEFONE_CLIENTE).contains("Mercado Bom Preco"));
    }

    @Test
    void naoEnviaQuandoATransacaoEDesfeita() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(evento().build());
            status.setRollbackOnly();
        });

        assertTrue(enviados.isEmpty());
    }

    @Test
    void semNumerosConfiguradosAindaConfirmaAoCliente() {
        definirTelefones("");

        whatsAppService.enviarAvisoPagamento(evento().build());

        assertEquals(1, enviados.size());
        corpoPara(TELEFONE_CLIENTE);
    }

    @Test
    void clienteSemTelefoneAindaAvisaOsNumeros() {
        whatsAppService.enviarAvisoPagamento(evento().telefoneCliente(null).build());

        assertEquals(2, enviados.size());
        corpoPara(TELEFONE_1);
        corpoPara(TELEFONE_2);
    }

    @Test
    void numeroInvalidoNaoImpedeOsDemais() {
        definirTelefones("123, " + TELEFONE_2);

        whatsAppService.enviarAvisoPagamento(evento().build());

        assertEquals(2, enviados.size());
        corpoPara(TELEFONE_2);
        corpoPara(TELEFONE_CLIENTE);
    }

    @Test
    void falhaEmUmDestinoNaoImpedeOsDemais() {
        AtomicInteger tentativas = new AtomicInteger();
        restTemplate.setRequestFactory((uri, method) -> {
            if (tentativas.getAndIncrement() == 0) {
                throw new IOException("evolution fora do ar");
            }
            MockClientHttpRequest request = new MockClientHttpRequest(method, uri);
            request.setResponse(new MockClientHttpResponse(new byte[0], HttpStatus.OK));
            enviados.add(request);
            return request;
        });

        assertDoesNotThrow(() -> whatsAppService.enviarAvisoPagamento(evento().build()));

        assertEquals(3, tentativas.get());
        corpoPara(TELEFONE_2);
        corpoPara(TELEFONE_CLIENTE);
    }
}
