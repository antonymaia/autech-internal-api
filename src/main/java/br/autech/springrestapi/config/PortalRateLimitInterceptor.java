package br.autech.springrestapi.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit por IP com janela deslizante de 60s.
 * Usado no "login" do portal (GET /pub/cliente/{cnpjCpf}) pra evitar
 * bot enumerando CPFs. Zero dependencia externa.
 */
@Slf4j
@Component
public class PortalRateLimitInterceptor implements HandlerInterceptor {

    private static final long JANELA_MS = 60_000L;

    @Value("${portal.rate-limit.per-minute:10}")
    private int limitePorMinuto;

    private final Map<String, Deque<Long>> timestampsPorIp = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String ip = extrairIp(request);
        long agora = System.currentTimeMillis();
        long limiteJanela = agora - JANELA_MS;

        Deque<Long> fila = timestampsPorIp.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && fila.peekFirst() < limiteJanela) {
                fila.pollFirst();
            }
            if (fila.size() >= limitePorMinuto) {
                long maisAntigo = fila.peekFirst();
                long retryAfterSegundos = Math.max(1, (JANELA_MS - (agora - maisAntigo)) / 1000);
                log.warn("[Portal] Rate limit excedido para IP {} (fila={}, retry={}s)",
                        ip, fila.size(), retryAfterSegundos);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", String.valueOf(retryAfterSegundos));
                return false;
            }
            fila.addLast(agora);
        }
        return true;
    }

    private String extrairIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int virgula = xff.indexOf(',');
            return (virgula > 0 ? xff.substring(0, virgula) : xff).trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return request.getRemoteAddr();
    }
}
