package br.com.fiap.ordenha.controller;

import br.com.fiap.ordenha.dto.CadastroRequest;
import br.com.fiap.ordenha.dto.CadastroResponse;
import br.com.fiap.ordenha.dto.LoginRequest;
import br.com.fiap.ordenha.dto.LoginResponse;
import br.com.fiap.ordenha.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação")
@SecurityRequirements // rotas públicas: não pedem token
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/cadastro")
    @Operation(summary = "Cria usuário (senha gravada só como hash BCrypt) e a primeira propriedade dele")
    public ResponseEntity<CadastroResponse> cadastrar(@Valid @RequestBody CadastroRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.cadastrar(req));
    }

    @PostMapping("/login")
    @Operation(summary = "Valida email e senha e devolve o token de acesso")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }
}
