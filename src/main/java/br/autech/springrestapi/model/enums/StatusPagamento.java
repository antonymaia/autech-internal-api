package br.autech.springrestapi.model.enums;

public enum StatusPagamento {

    APROVADO(1, "Aprovado"),
    PENDENTE(2, "Pendente"),
    CANCELADO(3, "Cancelado");

    private int cod;
    private String descricao;

    private StatusPagamento(int cod, String descricao) {
        this.cod = cod;
        this.descricao = descricao;
    }

    public int getCod() {
        return cod;
    }

    public String getDescricao () {
        return descricao;
    }

    public static StatusPagamento toEnum(Integer cod) {

        if (cod == null) {
            return null;
        }

        for (StatusPagamento x : StatusPagamento.values()) {
            if (cod.equals(x.getCod())) {
                return x;
            }
        }

        throw new IllegalArgumentException("Id inválido: " + cod);
    }
}
