package br.autech.springrestapi.controller;

import br.autech.springrestapi.dtos.CobrancaLoteDTO;
import br.autech.springrestapi.service.WhatsAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whatsapp")
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    public WhatsAppController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @PostMapping("/teste")
    public ResponseEntity<String> teste(@RequestParam String telefone) {
        whatsAppService.enviarMensagemTeste(telefone);
        return ResponseEntity.ok("Mensagem enviada para " + telefone);
    }

    /** Clientes da cobranca em lote: tipo AMANHA, HOJE ou VENCIDA (outro valor responde 400). */
    @GetMapping("/cobranca")
    public ResponseEntity<CobrancaLoteDTO> listarCobrancaLote(@RequestParam("tipo") WhatsAppService.TipoCobrancaLote tipo) {
        return ResponseEntity.ok(whatsAppService.listarCobrancaLote(tipo));
    }

    @GetMapping("/enviar_mensagem_cobranca/{cnpjCpf}")
    public ResponseEntity<Void> enviarMensagemCobranca(@PathVariable("cnpjCpf") String cnpjCpf){
        whatsAppService.enviarMensagemCobranca(cnpjCpf);
        return ResponseEntity.ok().build();
    }
}
