package br.com.fiap.ordenha.service;

import br.com.fiap.ordenha.dto.*;
import br.com.fiap.ordenha.exception.ConflitoException;
import br.com.fiap.ordenha.exception.NaoAutorizadoException;
import br.com.fiap.ordenha.model.Propriedade;
import br.com.fiap.ordenha.model.Usuario;
import br.com.fiap.ordenha.repository.PropriedadeRepository;
import br.com.fiap.ordenha.repository.UsuarioRepository;
import br.com.fiap.ordenha.security.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    /** Mesma mensagem para email inexistente e senha errada: não revela quais emails estão cadastrados. */
    private static final String LOGIN_INVALIDO = "Email ou senha inválidos";

    private final UsuarioRepository usuarioRepository;
    private final PropriedadeRepository propriedadeRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UsuarioRepository usuarioRepository, PropriedadeRepository propriedadeRepository,
                       PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.propriedadeRepository = propriedadeRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /** Cria o usuário (senha só como hash BCrypt) e a primeira propriedade dele. */
    @Transactional
    public CadastroResponse cadastrar(CadastroRequest req) {
        String email = normalizar(req.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflitoException("Já existe um usuário com este email");
        }
        Usuario usuario = new Usuario();
        usuario.setNome(req.nome().trim());
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(req.senha()));
        usuarioRepository.save(usuario);

        Propriedade propriedade = new Propriedade();
        propriedade.setUsuario(usuario);
        propriedade.setNome(req.propriedade().nome().trim());
        propriedade.setMunicipio(req.propriedade().municipio().trim());
        propriedade.setUf(req.propriedade().uf().toUpperCase(Locale.ROOT));
        propriedadeRepository.save(propriedade);

        return new CadastroResponse(UsuarioResponse.de(usuario), PropriedadeResponse.de(propriedade));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        Usuario usuario = usuarioRepository.findByEmail(normalizar(req.email()))
                .orElseThrow(() -> new NaoAutorizadoException(LOGIN_INVALIDO));
        if (!passwordEncoder.matches(req.senha(), usuario.getSenhaHash())) {
            throw new NaoAutorizadoException(LOGIN_INVALIDO);
        }
        String token = tokenService.gerar(usuario.getId());
        var propriedades = propriedadeRepository.findByUsuarioIdOrderByNome(usuario.getId()).stream()
                .map(PropriedadeResponse::de).toList();
        return new LoginResponse(token, tokenService.expiracao(token), UsuarioResponse.de(usuario), propriedades);
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
