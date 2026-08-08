package br.autech.springrestapi.model.enums;

public enum TipoPagamento {
    PIX("pix", "PIX"),
    DINHEIRO("dinheiro", "Dinheiro"),
    CARTAO_CREDITO("cartaoCredito", "Cartão de Crédito"),
    CARTAO_DEBITO("cartaoDebito", "Cartão de Débito"),
    BOLETO("boleto", "Boleto");

    private final String codigo;
    private final String descricao;

    TipoPagamento(String codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }
}
