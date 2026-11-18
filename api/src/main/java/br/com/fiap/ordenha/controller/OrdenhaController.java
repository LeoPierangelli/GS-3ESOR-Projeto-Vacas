package br.com.fiap.ordenha.controller;

import br.com.fiap.ordenha.dto.OrdenhaRegistradaResponse;
import br.com.fiap.ordenha.dto.OrdenhaRequest;
import br.com.fiap.ordenha.security.AutenticacaoFilter;
import br.com.fiap.ordenha.service.OrdenhaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ordenhas")
@Tag(name = "Ordenhas")
public class OrdenhaController {

    private final OrdenhaService ordenhaService;

    public OrdenhaController(OrdenhaService ordenhaService) {
        this.ordenhaService = ordenhaService;
    }

    @PostMapping
    @Operation(summary = "Recebe a leitura dos sensores, grava a ordenha e aplica as regras de alerta")
    public ResponseEntity<OrdenhaRegistradaResponse> registrar(@Valid @RequestBody OrdenhaRequest req,
                                                               @Parameter(hidden = true) @RequestAttribute(AutenticacaoFilter.ATRIBUTO_USUARIO) Long usuarioId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenhaService.registrar(req, usuarioId));
    }
}
