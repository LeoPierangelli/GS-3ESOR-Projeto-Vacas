package br.com.fiap.ordenha.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sessões de login. O token é um valor aleatório de 256 bits guardado em memória junto com o id do
 * usuário, no mesmo papel da sessão HTTP do estudo de caso (LoginServlet + filtro). Reiniciar a API
 * desloga todo mundo, o que é aceitável para o MVP.
 */
@Service
public class TokenService {

    private record Sessao(Long usuarioId, LocalDateTime expiraEm) {
    }

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Sessao> sessoes = new ConcurrentHashMap<>();
    private final long validadeHoras;

    public TokenService(@Value("${ordenha.token.validade-horas:8}") long validadeHoras) {
        this.validadeHoras = validadeHoras;
    }

    public String gerar(Long usuarioId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessoes.put(token, new Sessao(usuarioId, LocalDateTime.now().plusHours(validadeHoras)));
        return token;
    }

    public LocalDateTime expiracao(String token) {
        Sessao s = sessoes.get(token);
        return s == null ? null : s.expiraEm();
    }

    /** Devolve o id do usuário dono do token, se o token existir e não tiver expirado. */
    public Optional<Long> validar(String token) {
        if (token == null) {
            return Optional.empty();
        }
        Sessao s = sessoes.get(token);
        if (s == null) {
            return Optional.empty();
        }
        if (s.expiraEm().isBefore(LocalDateTime.now())) {
            sessoes.remove(token);
            return Optional.empty();
        }
        return Optional.of(s.usuarioId());
    }
}
