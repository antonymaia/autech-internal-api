package br.autech.springrestapi.service;

import br.autech.springrestapi.model.Cliente;
import br.autech.springrestapi.model.Fatura;
import br.autech.springrestapi.model.PagamentoDinheiro;
import br.autech.springrestapi.model.enums.EstadoFatura;
import br.autech.springrestapi.repository.FaturaRepository;
import br.autech.springrestapi.repository.PagamentoFaturaRepository;
import br.autech.springrestapi.repository.PagamentoRepository;
import br.autech.springrestapi.service.exception.BadRequestException;
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
class FaturaServicePagamentoManualTest {

    @Mock
    FaturaRepository faturaRepository;
    @Mock
    PagamentoRepository pagamentoRepository;
    @Mock
    PagamentoFaturaRepository pagamentoFaturaRepository;
    @Mock
    ApplicationEventPublisher eventPublisher;
    @InjectMocks
    FaturaService faturaService;

    private Fatura fatura;

    @BeforeEach
    void setUp() {
        Cliente cliente = new Cliente();
        cliente.setCnpjCpf("12345678000199");
        cliente.setNome("Mercado Bom Preco");
        cliente.setTelefone("81977776666");
        cliente.setBloqueado("N");

        fatura = new Fatura();
        fatura.setId(42L);
        fatura.setValor(new BigDecimal("100.00"));
        fatura.setEstado(EstadoFatura.FECHADA);
        fatura.setCliente(cliente);

        when(faturaRepository.findById(42L)).thenReturn(Optional.of(fatura));
        when(faturaRepository.save(any())).then(returnsFirstArg());
        when(pagamentoRepository.save(any())).then(returnsFirstArg());
        when(pagamentoFaturaRepository.save(any())).then(returnsFirstArg());
    }

    private PagamentoDinheiro dinheiro(String valor) {
        PagamentoDinheiro pagamento = new PagamentoDinheiro();
        pagamento.setValorPagamento(new BigDecimal(valor));
        return pagamento;
    }

    @Test
    void publicaEventoDeBaixaManualComAFormaDePagamento() {
        faturaService.adicionarPagamento(42L, dinheiro("100.00"));

        assertEquals(EstadoFatura.PAGA, fatura.getEstado());
        ArgumentCaptor<PagamentoConfirmadoEvent> captor = ArgumentCaptor.forClass(PagamentoConfirmadoEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        PagamentoConfirmadoEvent evento = captor.getValue();
        assertEquals(PagamentoConfirmadoEvent.Origem.MANUAL, evento.getOrigem());
        assertEquals("Dinheiro", evento.getFormaPagamento());
        assertEquals(42L, evento.getFaturaId());
        assertEquals("Mercado Bom Preco", evento.getNomeCliente());
        assertEquals(new BigDecimal("100.00"), evento.getValor());
        assertNotNull(evento.getDataPagamento());
    }

    @Test
    void pagamentoRecusadoNaoPublicaEvento() {
        assertThrows(BadRequestException.class, () -> faturaService.adicionarPagamento(42L, dinheiro("50.00")));

        verifyNoInteractions(eventPublisher);
    }
}
