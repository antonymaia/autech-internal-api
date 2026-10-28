package br.autech.springrestapi.controller;

import br.autech.springrestapi.ScheduledTasks.PagamentoTask;
import br.autech.springrestapi.dtos.GerarFaturaResponse;
import br.autech.springrestapi.model.Fatura;
import br.autech.springrestapi.model.Pagamento;
import br.autech.springrestapi.service.FaturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("fatura")
@RequiredArgsConstructor
public class FaturaController {
    private final FaturaService faturaService;
    private final PagamentoTask pagamentoTask;

    @PostMapping("/adicionar-pagamento/{faturaid}")
    public ResponseEntity<Fatura> adicionarPagamento(@PathVariable Long faturaid, @RequestBody Pagamento pagamento){
        return ResponseEntity.ok(faturaService.adicionarPagamento(faturaid, pagamento));
    }

    // Roda o mesmo fechamento do cron das 05:00 (fecha, cria a proxima e envia os avisos)
    @GetMapping("/gerar-faturas")
    public ResponseEntity<Void> gerarFaturas(){
        pagamentoTask.fecharFaturas();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cliente/{cnpjCpf}/gerar")
    public ResponseEntity<GerarFaturaResponse> gerarManual(@PathVariable String cnpjCpf){
        GerarFaturaResponse resp = faturaService.gerarFaturaManual(cnpjCpf);
        HttpStatus status = resp.isGerada() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(resp);
    }

    @GetMapping("/cliente/{cnpjCpf}")
    public ResponseEntity<List<Fatura>> buscarFaturasCliente(@PathVariable String cnpjCpf){
        return ResponseEntity.ok(faturaService.buscarFaturasCliente(cnpjCpf));
    }

    @GetMapping("/atualizar_estado")
    public ResponseEntity<Void> atualizarEstado(){
        faturaService.atualizarEstadoFatura();
        return ResponseEntity.ok().build();
    }
}
