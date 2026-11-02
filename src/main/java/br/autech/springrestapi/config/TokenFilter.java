package br.autech.springrestapi.config;

import br.autech.springrestapi.service.TokenService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Autentica a requisição pelo "Authorization: Bearer <token>" do login dos colaboradores. Token inválido não barra
 * nada aqui: o site manda o token do portal do cliente para esta API também, e as rotas abertas têm que continuar
 * funcionando. Quem barra é o SecurityConfig, nas rotas que exigem permissão.
 *
 * Sem @Component de propósito: o Spring Boot registraria o filtro também fora do Spring Security.
 */
@RequiredArgsConstructor
public class TokenFilter extends OncePerRequestFilter {

    public static final String PUBLICAR_DOWNLOADS = "PUBLICAR_DOWNLOADS";
    public static final String VER_EXECUCOES = "VER_EXECUCOES";

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho != null && cabecalho.startsWith("Bearer ")) {
            Claims claims = tokenService.validar(cabecalho.substring(7));
            if (claims != null) {
                List<GrantedAuthority> permissoes = new ArrayList<>();
                if (Boolean.TRUE.equals(claims.get("publicar", Boolean.class))) {
                    permissoes.add(new SimpleGrantedAuthority(PUBLICAR_DOWNLOADS));
                }
                if (Boolean.TRUE.equals(claims.get("verExecucoes", Boolean.class))) {
                    permissoes.add(new SimpleGrantedAuthority(VER_EXECUCOES));
                }
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(claims.get("login", String.class), null, permissoes));
            }
        }
        chain.doFilter(request, response);
    }
}
