package br.autech.springrestapi.model.enums;

public enum FormaPagamento {

    DINHEIRO(1, "Dinheiro"),
    CARTAO_DEBITO( 2, "Cartão débito"),
    CARTAO_CREDITO(3, "Cartão crédito"),
    PIX(4, "PIX"),
    TRANFERENCIA_BANCARIA(5, "Tranferência bancária");

    private int cod;
    private String descricao;

    private FormaPagamento(int cod, String descricao){
        this.cod = cod;
        this.descricao = descricao;
    }

    public int getCod() {
        return cod;
    }

    public String getDescricao () {
        return descricao;
    }

    public static FormaPagamento toEnum(Integer cod) {

        if (cod == null) {
            return null;
        }

        for (FormaPagamento x : FormaPagamento.values()) {
            if (cod.equals(x.getCod())) {
                return x;
            }
        }

        throw new IllegalArgumentException("Id inválido: " + cod);
    }
}
