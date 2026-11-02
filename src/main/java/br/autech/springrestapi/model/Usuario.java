package br.autech.springrestapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "USUARIOS")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String login;
    /** Só entra (login e cadastro); o hash nunca sai nas respostas. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String senha;
    /** Pode enviar e substituir arquivos no servidor de downloads. */
    @Column(name = "PODE_PUBLICAR")
    private Boolean podePublicar;
    /** Pode consultar as execuções das tarefas agendadas e o log de cobrança. */
    @Column(name = "PODE_VER_EXECUCOES")
    private Boolean podeVerExecucoes;
    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    public Usuario(){}

    public Usuario(Long id, String login, String senha, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.login = login;
        this.senha = senha;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
