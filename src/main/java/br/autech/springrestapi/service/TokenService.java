package br.autech.springrestapi.service;

import br.autech.springrestapi.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

/**
 * Token (JWT) do login dos colaboradores. Leva o id, o login e se o usuário pode publicar arquivos no servidor de
 * downloads.
 */
@Slf4j
@Service
public class TokenService {

    private static final String EMISSOR = "internal-admin-api";

    private final Key chave;
    private final long validadeMs;

    public TokenService(@Value("${jwt.secret:}") String segredo,
                        @Value("${jwt.validade-horas:12}") long validadeHoras) {
        if (segredo == null || segredo.isBlank()) {
            log.warn("[Token] jwt.secret (JWT_SECRET) vazio: usando um segredo aleatório, os tokens deixam de valer quando a API reiniciar");
            this.chave = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        } else {
            this.chave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(segredo));
        }
        this.validadeMs = validadeHoras * 3_600_000L;
    }

    public String gerar(Usuario usuario) {
        Date agora = new Date();
        return Jwts.builder()
                .setIssuer(EMISSOR)
                .setSubject(String.valueOf(usuario.getId()))
                .claim("login", usuario.getLogin())
                .claim("publicar", Boolean.TRUE.equals(usuario.getPodePublicar()))
                .setIssuedAt(agora)
                .setExpiration(new Date(agora.getTime() + validadeMs))
                .signWith(chave, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Claims do token, ou null se for inválido ou expirado (inclusive o do portal do cliente, que chega junto). */
    public Claims validar(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(chave).requireIssuer(EMISSOR).build()
                    .parseClaimsJws(token).getBody();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
