package br.autech.springrestapi.service;

import br.autech.springrestapi.dtos.ClienteDevedorDTO;
import br.autech.springrestapi.model.Cliente;
import br.autech.springrestapi.model.Fatura;
import br.autech.springrestapi.model.enums.EstadoFatura;
import br.autech.springrestapi.repository.FaturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;

class FaturaServiceDevedoresTest {

    static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    FaturaRepository repository;
    FaturaService service;
    List<Fatura> faturas;
    long proximoId;

    @BeforeEach
    void preparar() {
        repository = Mockito.mock(FaturaRepository.class);
        service = new FaturaService(repository, null, null, null, null, null, null, null, null, null, null, null);
        faturas = new ArrayList<>();
        proximoId = 1;

        // O mock responde como o banco: filtra a lista de faturas pelos mesmos criterios da consulta
        Mockito.when(repository.findAllByDataVencimentoLessThanEqualAndEstadoIn(any(), anyCollection())).thenAnswer(inv -> {
            LocalDate data = inv.getArgument(0);
            List<Integer> estados = new ArrayList<>(inv.getArgument(1));
            return faturas.stream()
                .filter(f -> !f.getDataVencimento().isAfter(data) && estados.contains(f.getEstado().getCod()))
                .collect(Collectors.toList());
        });
    }

    private Cliente cliente(String cnpjCpf, String nome, String ativo) {
        Cliente cliente = new Cliente();
        cliente.setCnpjCpf(cnpjCpf);
        cliente.setNome(nome);
        cliente.setAtivo(ativo);
        cliente.setBloqueado("N");
        return cliente;
    }

    private Fatura fatura(Cliente cliente, LocalDate vencimento, EstadoFatura estado, String valor) {
        Fatura fatura = new Fatura(proximoId++, estado, new BigDecimal(valor), vencimento, null, null, cliente, null);
        faturas.add(fatura);
        return fatura;
    }

    private static List<String> nomes(List<ClienteDevedorDTO> devedores) {
        return devedores.stream().map(ClienteDevedorDTO::getNome).collect(Collectors.toList());
    }

    @Test
    void listaSoQuemTemFaturaPendenteComVencimentoAnteriorAHoje() {
        Cliente padaria = cliente("1", "Padaria", "S");
        Cliente mercado = cliente("2", "Mercado", "S");
        Cliente farmacia = cliente("3", "Farmacia", "S");
        Cliente bar = cliente("4", "Bar", "S");
        fatura(padaria, HOJE.minusDays(1), EstadoFatura.VENCIDA, "100.00");
        fatura(mercado, HOJE, EstadoFatura.FECHADA, "100.00");            // vence hoje: ainda nao deve
        fatura(farmacia, HOJE.minusDays(40), EstadoFatura.PAGA, "100.00"); // ja pagou
        fatura(bar, HOJE.plusDays(20), EstadoFatura.ABERTA, "100.00");     // ainda nao fechou

        assertEquals(List.of("Padaria"), nomes(service.listarClientesDevedores()));
    }

    @Test
    void somaAsFaturasDoClienteEContaOAtrasoPelaMaisAntiga() {
        Cliente padaria = cliente("1", "Padaria", "S");
        Fatura recente = fatura(padaria, HOJE.minusDays(5), EstadoFatura.FECHADA, "50.50");
        Fatura antiga = fatura(padaria, HOJE.minusDays(35), EstadoFatura.VENCIDA, "100.00");

        List<ClienteDevedorDTO> devedores = service.listarClientesDevedores();

        assertEquals(1, devedores.size());
        ClienteDevedorDTO devedor = devedores.get(0);
        assertEquals(2, devedor.getQuantidadeFaturas());
        assertEquals(new BigDecimal("150.50"), devedor.getValorTotal());
        assertEquals(HOJE.minusDays(35), devedor.getVencimentoMaisAntigo());
        assertEquals(35, devedor.getDiasAtraso());
        assertEquals(List.of(antiga.getId(), recente.getId()),
            devedor.getFaturas().stream().map(ClienteDevedorDTO.FaturaDevida::getId).collect(Collectors.toList()));
        assertEquals(5, devedor.getFaturas().get(1).getDiasAtraso());
    }

    @Test
    void comecaPorQuemDeveHaMaisTempo() {
        fatura(cliente("1", "Padaria", "S"), HOJE.minusDays(3), EstadoFatura.VENCIDA, "100.00");
        fatura(cliente("2", "Mercado", "S"), HOJE.minusDays(60), EstadoFatura.VENCIDA, "100.00");
        fatura(cliente("3", "Acougue", "S"), HOJE.minusDays(3), EstadoFatura.VENCIDA, "100.00");

        assertEquals(List.of("Mercado", "Acougue", "Padaria"), nomes(service.listarClientesDevedores()));
    }

    @Test
    void incluiClienteInativoQueFicouDevendo() {
        fatura(cliente("1", "Padaria", "S"), HOJE.minusDays(3), EstadoFatura.VENCIDA, "100.00");
        fatura(cliente("2", "Mercado", "N"), HOJE.minusDays(60), EstadoFatura.VENCIDA, "100.00");

        List<ClienteDevedorDTO> devedores = service.listarClientesDevedores();

        assertEquals(List.of("Mercado", "Padaria"), nomes(devedores));
        assertEquals("N", devedores.get(0).getAtivo());
    }
}
