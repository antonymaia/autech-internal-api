package br.autech.springrestapi.service;

import br.autech.springrestapi.dtos.CobrancaLoteDTO;
import br.autech.springrestapi.model.Cliente;
import br.autech.springrestapi.model.Fatura;
import br.autech.springrestapi.model.enums.EstadoFatura;
import br.autech.springrestapi.repository.FaturaRepository;
import br.autech.springrestapi.service.WhatsAppService.TipoCobrancaLote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class WhatsAppServiceCobrancaLoteTest {

    static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    static final LocalDate AMANHA = HOJE.plusDays(1);

    FaturaRepository repository;
    WhatsAppService service;
    List<Fatura> faturas;
    long proximoId;

    @BeforeEach
    void preparar() {
        repository = Mockito.mock(FaturaRepository.class);
        service = new WhatsAppService(repository);
        faturas = new ArrayList<>();
        proximoId = 1;

        // O mock responde como o banco: filtra a lista de faturas pelos mesmos criterios das consultas
        Mockito.when(repository.findAllByDataVencimentoAndEstadoIn(any(), anyCollection())).thenAnswer(inv -> {
            LocalDate data = inv.getArgument(0);
            List<Integer> estados = new ArrayList<>(inv.getArgument(1));
            return faturas.stream()
                .filter(f -> f.getDataVencimento().isEqual(data) && estados.contains(f.getEstado().getCod()))
                .collect(Collectors.toList());
        });
        Mockito.when(repository.findAllByDataVencimentoLessThanEqualAndEstadoIn(any(), anyCollection())).thenAnswer(inv -> {
            LocalDate data = inv.getArgument(0);
            List<Integer> estados = new ArrayList<>(inv.getArgument(1));
            return faturas.stream()
                .filter(f -> !f.getDataVencimento().isAfter(data) && estados.contains(f.getEstado().getCod()))
                .collect(Collectors.toList());
        });
        Mockito.when(repository.findAllByCliente_CnpjCpfAndEstadoInOrderByDataVencimento(anyString(), anyCollection())).thenAnswer(inv -> {
            String cnpjCpf = inv.getArgument(0);
            List<Integer> estados = new ArrayList<>(inv.getArgument(1));
            return faturas.stream()
                .filter(f -> f.getCliente().getCnpjCpf().equals(cnpjCpf) && estados.contains(f.getEstado().getCod()))
                .sorted(Comparator.comparing(Fatura::getDataVencimento))
                .collect(Collectors.toList());
        });
    }

    private Cliente cliente(String cnpjCpf, String nome, String telefone) {
        Cliente cliente = new Cliente();
        cliente.setCnpjCpf(cnpjCpf);
        cliente.setNome(nome);
        cliente.setTelefone(telefone);
        cliente.setAtivo("S");
        return cliente;
    }

    private Fatura fatura(Cliente cliente, LocalDate vencimento, EstadoFatura estado, String valor) {
        Fatura fatura = new Fatura(proximoId++, estado, new BigDecimal(valor), vencimento, null, null, cliente, null);
        faturas.add(fatura);
        return fatura;
    }

    private static List<String> nomes(List<CobrancaLoteDTO.Cliente> clientes) {
        return clientes.stream().map(CobrancaLoteDTO.Cliente::getNome).collect(Collectors.toList());
    }

    @Test
    void amanhaListaSoQuemTemAFaturaMaisAntigaVencendoAmanha() {
        Cliente padaria = cliente("1", "Padaria", "81988881111");
        Cliente mercado = cliente("2", "Mercado", "81988882222");
        Cliente loja = cliente("3", "Loja", "81988883333");
        Cliente farmacia = cliente("4", "Farmacia", "81988884444");
        Cliente pet = cliente("5", "Pet", "81988885555");
        fatura(padaria, AMANHA, EstadoFatura.FECHADA, "99.90");
        fatura(mercado, AMANHA, EstadoFatura.ABERTA, "150");
        fatura(loja, AMANHA, EstadoFatura.FECHADA, "120");
        fatura(loja, HOJE.minusDays(4), EstadoFatura.VENCIDA, "80");
        fatura(farmacia, AMANHA, EstadoFatura.PAGA, "120");
        fatura(pet, AMANHA.plusDays(1), EstadoFatura.ABERTA, "120");

        CobrancaLoteDTO resposta = service.listarCobrancaLote(TipoCobrancaLote.AMANHA);

        assertEquals(AMANHA, resposta.getDataReferencia());
        assertEquals(List.of("Mercado", "Padaria"), nomes(resposta.getClientes()));
        CobrancaLoteDTO.Cliente item = resposta.getClientes().get(1);
        assertEquals(new BigDecimal("99.90"), item.getValor());
        assertEquals(AMANHA, item.getVencimento());
        assertEquals(0, item.getDiasAtraso());

        assertEquals(1, resposta.getComFaturaAnterior().size());
        assertEquals("Loja", resposta.getComFaturaAnterior().get(0).getNome());
        assertTrue(resposta.getComFaturaAnterior().get(0).getMotivo().contains("em aberto"));
        assertTrue(resposta.getSemTelefone().isEmpty());
    }

    @Test
    void separaClienteSemTelefoneEIgnoraInativo() {
        Cliente semTelefone = cliente("1", "Bar", " ");
        Cliente inativo = cliente("2", "Inativo", "81988882222");
        inativo.setAtivo("N");
        fatura(semTelefone, HOJE, EstadoFatura.FECHADA, "120");
        fatura(inativo, HOJE, EstadoFatura.FECHADA, "120");

        CobrancaLoteDTO resposta = service.listarCobrancaLote(TipoCobrancaLote.HOJE);

        assertEquals(HOJE, resposta.getDataReferencia());
        assertTrue(resposta.getClientes().isEmpty());
        assertEquals(1, resposta.getSemTelefone().size());
        assertEquals("Sem telefone no cadastro", resposta.getSemTelefone().get(0).getMotivo());
        assertTrue(resposta.getComFaturaAnterior().isEmpty());
    }

    @Test
    void vencidaListaUmaVezCadaClienteComAFaturaMaisAntigaEOsDiasDeAtraso() {
        Cliente loja = cliente("1", "Loja", "81988881111");
        Cliente oficina = cliente("2", "Oficina", "81988882222");
        fatura(loja, HOJE.minusDays(40), EstadoFatura.VENCIDA, "70");
        fatura(loja, HOJE.minusDays(10), EstadoFatura.VENCIDA, "80");
        fatura(oficina, HOJE, EstadoFatura.FECHADA, "200");

        CobrancaLoteDTO resposta = service.listarCobrancaLote(TipoCobrancaLote.VENCIDA);

        assertEquals(List.of("Loja"), nomes(resposta.getClientes()));
        CobrancaLoteDTO.Cliente item = resposta.getClientes().get(0);
        assertEquals(HOJE.minusDays(40), item.getVencimento());
        assertEquals(40, item.getDiasAtraso());
        assertEquals(new BigDecimal("70"), item.getValor());
    }

    @Test
    void envioManualContinuaCobrandoAFaturaMaisAntiga() {
        Cliente loja = cliente("1", "Loja", null);
        fatura(loja, AMANHA, EstadoFatura.FECHADA, "120");

        // Sem telefone o envio falha, mas so depois de achar a fatura: sem fatura pendente a mensagem e outra
        var erro = assertThrows(RuntimeException.class, () -> service.enviarMensagemCobranca("1"));
        assertEquals("Mensagem não enviada, verifique os dados do cliente", erro.getMessage());
        var semFatura = assertThrows(RuntimeException.class, () -> service.enviarMensagemCobranca("9"));
        assertEquals("Mensagem não enviada, cliente não possui fatura pendente", semFatura.getMessage());
        Mockito.verify(repository, Mockito.atLeastOnce())
            .findAllByCliente_CnpjCpfAndEstadoInOrderByDataVencimento(eq("1"), anyCollection());
    }
}
