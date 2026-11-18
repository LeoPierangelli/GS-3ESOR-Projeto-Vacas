package br.com.fiap.ordenha.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Filtro de login, como o do estudo de caso: toda rota fora de /auth (e da documentação) exige o
 * cabeçalho "Authorization: Bearer &lt;token&gt;". Sem token válido, responde 401 e a requisição
 * nem chega ao controller. Com token válido, o id do usuário vai como atributo da requisição.
 */
@Component
public class AutenticacaoFilter extends OncePerRequestFilter {

    public static final String ATRIBUTO_USUARIO = "usuarioId";

    private static final List<String> ROTAS_PUBLICAS = List.of("/auth/", "/swagger-ui", "/v3/api-docs", "/error");

    private final TokenService tokenService;

    public AutenticacaoFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getRequestURI();
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || ROTAS_PUBLICAS.stream().anyMatch(caminho::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");

        String cabecalho = request.getHeader("Authorization");
        String token = (cabecalho != null && cabecalho.startsWith("Bearer ")) ? cabecalho.substring(7).trim() : null;
        Optional<Long> usuarioId = tokenService.validar(token);

        if (usuarioId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"status\":401,\"erro\":\"Unauthorized\","
                    + "\"mensagem\":\"Faça login e envie o token no cabeçalho Authorization: Bearer <token>\","
                    + "\"timestamp\":\"" + LocalDateTime.now() + "\"}");
            return;
        }

        request.setAttribute(ATRIBUTO_USUARIO, usuarioId.get());
        chain.doFilter(request, response);
    }
}
