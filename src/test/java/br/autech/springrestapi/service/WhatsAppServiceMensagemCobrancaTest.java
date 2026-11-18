package br.autech.springrestapi.service;

import br.autech.springrestapi.repository.FaturaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhatsAppServiceMensagemCobrancaTest {

    private static final String CNPJ = "49114818000124";
    private static final String LINK = "https://pagar.autech.net.br/login#" + CNPJ;

    @ParameterizedTest
    @EnumSource(WhatsAppService.AvisoFatura.class)
    void documentoApareceSomenteDentroDoLinkDoPortal(WhatsAppService.AvisoFatura aviso) {
        String mensagem = montar(aviso);

        assertTrue(mensagem.contains(LINK));
        assertEquals(mensagem.indexOf(CNPJ), mensagem.indexOf(LINK) + LINK.length() - CNPJ.length());
        assertEquals(mensagem.lastIndexOf(CNPJ), mensagem.indexOf(CNPJ));
        assertFalse(mensagem.contains("informe seu CPF/CNPJ"));
    }

    @Test
    void orientaPagarSomentePeloCodigoGeradoNoPortal() {
        assertTrue(montar(WhatsAppService.AvisoFatura.ATRASO)
            .contains("Pague somente pelo código PIX gerado no portal."));
    }

    private String montar(WhatsAppService.AvisoFatura aviso) {
        WhatsAppService service = new WhatsAppService(Mockito.mock(FaturaRepository.class));
        ReflectionTestUtils.setField(service, "portalUrl", "https://pagar.autech.net.br");
        return ReflectionTestUtils.invokeMethod(service, "montarMensagem",
            aviso, "BEATRIZ", CNPJ, "05/11/2026", "R$ 135,00");
    }
}
