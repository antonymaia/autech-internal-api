package br.autech.springrestapi.config;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Autentica consumidores sem login (outras APIs e apps locais) pelo cabeçalho "X-API-Key". As chaves vêm de
 * api.chaves (API_CHAVES) no formato "consumidor:chave,outro:chave2"; o nome do consumidor vira o principal, então
 * cada um tem a sua chave e dá para revogar uma sem mexer nas outras. Chave ausente ou errada não barra nada aqui:
 * quem barra é o SecurityConfig, nas rotas que exigem API_EXTERNA.
 *
 * Sem @Component de propósito: o Spring Boot registraria o filtro também fora do Spring Security.
 */
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String CABECALHO = "X-API-Key";
    public static final String API_EXTERNA = "API_EXTERNA";

    /** Hash SHA-256 da chave -> nome do consumidor. */
    private final Map<String, String> consumidores = new LinkedHashMap<>();
    private final List<SimpleGrantedAuthority> permissoes = List.of(new SimpleGrantedAuthority(API_EXTERNA));

    public ApiKeyFilter(String configuracao) {
        if (configuracao == null) return;
        for (String item : configuracao.split(",")) {
            String trim = item.trim();
            if (trim.isEmpty()) continue;
            int sep = trim.indexOf(':');
            if (sep <= 0 || sep == trim.length() - 1)
                throw new IllegalStateException("api.chaves inválida, use o formato consumidor:chave separados por vírgula");
            String chave = trim.substring(sep + 1).trim();
            if (chave.length() < 32)
                throw new IllegalStateException("A chave do consumidor '" + trim.substring(0, sep) + "' deve ter ao menos 32 caracteres");
            consumidores.put(hash(chave), trim.substring(0, sep).trim());
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String chave = request.getHeader(CABECALHO);
        if (chave != null && !chave.isBlank()) {
            String consumidor = buscarConsumidor(chave.trim());
            if (consumidor != null) {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken("api:" + consumidor, null, permissoes));
            }
        }
        chain.doFilter(request, response);
    }

    /** Compara os hashes em tempo constante, sem vazar pelo tempo de resposta quanto da chave acertou. */
    private String buscarConsumidor(String chave) {
        byte[] recebido = hash(chave).getBytes(StandardCharsets.US_ASCII);
        String encontrado = null;
        for (Map.Entry<String, String> e : consumidores.entrySet()) {
            if (MessageDigest.isEqual(recebido, e.getKey().getBytes(StandardCharsets.US_ASCII))) {
                encontrado = e.getValue();
            }
        }
        return encontrado;
    }

    private static String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
