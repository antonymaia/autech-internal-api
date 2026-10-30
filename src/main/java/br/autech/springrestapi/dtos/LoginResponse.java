package br.autech.springrestapi.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Resposta do POST /usuario/login: o token vai no "Authorization: Bearer" das rotas protegidas. */
@Getter
@AllArgsConstructor
public class LoginResponse {
    private Long id;
    private String login;
    private boolean podePublicar;
    private String token;
}
