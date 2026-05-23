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
    public ResponseEntity<String> teste(@RequestParam String telefone, @RequestParam String mensagem) {
        whatsAppService.enviarMensagemTeste(telefone, mensagem);
        return ResponseEntity.ok("Mensagem enviada para " + telefone);
    }
}
