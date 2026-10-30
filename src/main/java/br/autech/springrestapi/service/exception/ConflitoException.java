package br.autech.springrestapi.service.exception;

public class ConflitoException extends RuntimeException{
    private static final long serialVersionUID = 1L;

    public ConflitoException(String msg) {
        super(msg);
    }

    public ConflitoException(String msg, Throwable cause) {
        super(msg,cause);
    }

}
