package br.autech.springrestapi.model.enums;

public enum StatusAssinatura {
    ATIVA(1,  "Ativa") ,

     CANCELADA( 2, "Cancelada"),

    SUSPENSA( 3 , "Suspensa");

    private int cod ;
    private String descricao;

    private StatusAssinatura(int cod, String descricao) {
        this.cod = cod;
        this.descricao = descricao;
    }

    public int getCod() {
        return cod;
    }


    public String getDescricao() {
        return descricao;
    }

}
