package br.autech.springrestapi.service.exception;

public class InternalServerError extends RuntimeException {
   public InternalServerError(String message) {
      super(message);
   }

   public InternalServerError(String msg, Throwable cause) {
      super(msg, cause);
   }
}
