package br.autech.springrestapi.controller;

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

    @GetMapping("/enviar_mensagem_cobranca/{cnpjCpf}")
    public ResponseEntity<Void> enviarMensagemCobranca(@PathVariable("cnpjCpf") String cnpjCpf){
        whatsAppService.enviarMensagemCobranca(cnpjCpf);
        return ResponseEntity.ok().build();
    }
}
