package br.autech.springrestapi.service;

import br.autech.springrestapi.model.Cliente;
import br.autech.springrestapi.model.Fatura;
import br.autech.springrestapi.model.enums.EstadoFatura;
import br.autech.springrestapi.repository.FaturaRepository;
import br.autech.springrestapi.repository.PagamentoFaturaRepository;
import br.autech.springrestapi.repository.PagamentoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalServiceWebhookTest {

    @Mock
    FaturaRepository faturaRepository;
    @Mock
    PagamentoRepository pagamentoRepository;
    @Mock
    PagamentoFaturaRepository pagamentoFaturaRepository;
    @Mock
    MercadoPagoService mercadoPagoService;
    @Mock
    FaturaService faturaService;
    @Mock
    ClienteService clienteService;
    @Mock
    ApplicationEventPublisher eventPublisher;
    @InjectMocks
    PortalService portalService;

    private Fatura fatura;

    @BeforeEach
    void setUp() {
        Cliente cliente = new Cliente();
        cliente.setCnpjCpf("12345678000199");
        cliente.setNome("Mercado Bom Preco");
        cliente.setNomeResponsavel("Maria");
        cliente.setTelefone("81977776666");
        cliente.setBloqueado("S");

        fatura = new Fatura();
        fatura.setId(42L);
        fatura.setValor(new BigDecimal("100.00"));
        fatura.setEstado(EstadoFatura.FECHADA);
        fatura.setCliente(cliente);

        when(faturaRepository.findById(42L)).thenReturn(Optional.of(fatura));
        when(pagamentoRepository.save(any())).then(returnsFirstArg());
        when(pagamentoFaturaRepository.save(any())).then(returnsFirstArg());
    }

    private JsonNode order(String status) throws JsonProcessingException {
        return new ObjectMapper().readTree("{\"id\":\"ORD1\",\"status\":\"" + status
            + "\",\"external_reference\":\"fatura-42\",\"total_amount\":\"135.90\"}");
    }

    @Test
    void publicaEventoQuandoAFaturaEPaga() throws Exception {
        when(mercadoPagoService.buscarOrder("ORD1")).thenReturn(order("processed"));

        assertTrue(portalService.processarWebhook("ORD1", "order"));

        assertEquals(EstadoFatura.PAGA, fatura.getEstado());
        PagamentoConfirmadoEvent evento = eventoPublicado();
        assertEquals(42L, evento.getFaturaId());
        assertEquals("12345678000199", evento.getCnpjCpf());
        assertEquals("Mercado Bom Preco", evento.getNomeCliente());
        assertEquals("Maria", evento.getNomeResponsavel());
        assertEquals("81977776666", evento.getTelefoneCliente());
        assertEquals(new BigDecimal("135.90"), evento.getValor());
        assertNotNull(evento.getDataPagamento());
        assertEquals(PagamentoConfirmadoEvent.Origem.MERCADO_PAGO, evento.getOrigem());
        assertEquals("PIX", evento.getFormaPagamento());
    }

    private PagamentoConfirmadoEvent eventoPublicado() {
        ArgumentCaptor<PagamentoConfirmadoEvent> captor = ArgumentCaptor.forClass(PagamentoConfirmadoEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    @Test
    void eventoInformaClienteAindaBloqueadoQuandoHaOutraFaturaEmAtraso() throws Exception {
        when(mercadoPagoService.buscarOrder("ORD1")).thenReturn(order("processed"));

        portalService.processarWebhook("ORD1", "order");

        assertTrue(eventoPublicado().isClienteBloqueado());
    }

    @Test
    void eventoInformaClienteLiberadoQuandoOPagamentoDesbloqueia() throws Exception {
        when(mercadoPagoService.buscarOrder("ORD1")).thenReturn(order("processed"));
        doAnswer(invocation -> {
            invocation.<Cliente>getArgument(0).setBloqueado("N");
            return null;
        }).when(faturaService).desbloquearSeQuitado(any());

        portalService.processarWebhook("ORD1", "order");

        assertFalse(eventoPublicado().isClienteBloqueado());
    }

    @Test
    void naoPublicaEventoParaWebhookDuplicado() throws Exception {
        fatura.setEstado(EstadoFatura.PAGA);
        when(mercadoPagoService.buscarOrder("ORD1")).thenReturn(order("processed"));

        assertFalse(portalService.processarWebhook("ORD1", "order"));

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void naoPublicaEventoEnquantoOPagamentoNaoEConfirmado() throws Exception {
        when(mercadoPagoService.buscarOrder("ORD1")).thenReturn(order("action_required"));

        assertFalse(portalService.processarWebhook("ORD1", "order"));

        verifyNoInteractions(eventPublisher);
    }
}
