package br.autech.springrestapi.model.enums;

public enum StatusExecucaoTarefa {
    /** Rodando, ou a API caiu no meio da execução. */
    EM_ANDAMENTO,
    SUCESSO,
    /** Terminou, mas algum cliente deu erro. */
    COM_ERROS,
    /** Parou antes de terminar. */
    FALHA
}
