package br.autech.springrestapi.model.enums;

public enum EstadoFatura {
    ABERTA(1, "Aberta"),
    PAGA(2, "Paga"),
    VENCIDA(3, "Vencida");

    private int cod;
    private String descricao;

    private EstadoFatura(int cod, String descricao){
        this.cod = cod;
        this.descricao = descricao;
    }

    public int getCod() {
        return cod;
    }

    public String getDescricao () {
        return descricao;
    }

    public static EstadoFatura toEnum(Integer cod) {

        if (cod == null) {
            return null;
        }

        for (EstadoFatura x : EstadoFatura.values()) {
            if (cod.equals(x.getCod())) {
                return x;
            }
        }

        throw new IllegalArgumentException("Id inválido: " + cod);
    }
}
