package br.autech.springrestapi.service;

import br.autech.springrestapi.dtos.HomePageData;
import br.autech.springrestapi.repository.ClienteRepository;
import br.autech.springrestapi.repository.PagamentoFaturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class PageDataService {
   private final ClienteRepository clienteRepository;
   private final PagamentoFaturaRepository pagamentoFaturaRepository;

   public HomePageData getHomePageData() {
      // Mesmo relógio do createdAt do pagamento (LocalDateTime.now() no fuso do servidor).
      YearMonth mesAtual = YearMonth.now();
      return HomePageData.builder()
         .qtdeClientesAtivos(clienteRepository.numberActiveCostomers())
         .qtdeClientesBloqueados(clienteRepository.numberBlockedCustomers())
         .rendaAtual(clienteRepository.getTotalIncome())
         .arrecadacaoMes(pagamentoFaturaRepository.somarPagamentosEntre(
            mesAtual.atDay(1).atStartOfDay(),
            mesAtual.plusMonths(1).atDay(1).atStartOfDay()))
         .build();
   }
}
