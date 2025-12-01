package br.autech.springrestapi.service.exception;

public class AssinaturaException {
    public static class JaExisteException extends RuntimeException{
        public JaExisteException(String cpfCnpj){
            super("Ja existe uma assinatura com esse CPF/CNPJ : " + cpfCnpj);
        }
    }
}
