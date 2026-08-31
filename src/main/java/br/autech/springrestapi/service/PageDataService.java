package br.autech.springrestapi.service;

import br.autech.springrestapi.dtos.HomePageData;
import br.autech.springrestapi.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PageDataService {
   private final ClienteRepository clienteRepository;

   public HomePageData getHomePageData() {
      return HomePageData.builder()
         .qtdeClientesAtivos(clienteRepository.numberActiveCostomers())
         .qtdeClientesBloqueados(clienteRepository.numberBlockedCustomers())
         .rendaAtual(clienteRepository.getTotalIncome())
         .build();
   }
}
