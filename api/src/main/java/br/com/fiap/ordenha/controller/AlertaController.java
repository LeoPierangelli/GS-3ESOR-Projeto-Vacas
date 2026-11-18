package br.com.fiap.ordenha.controller;

import br.com.fiap.ordenha.dto.AlertaResponse;
import br.com.fiap.ordenha.security.AutenticacaoFilter;
import br.com.fiap.ordenha.service.AlertaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alertas")
@Tag(name = "Alertas")
public class AlertaController {

    private final AlertaService alertaService;

    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    @GetMapping
    @Operation(summary = "Lista os alertas do rebanho, do mais recente ao mais antigo; filtre com ?resolvido=false")
    public List<AlertaResponse> listar(@RequestParam(required = false) Boolean resolvido,
                                       @Parameter(hidden = true) @RequestAttribute(AutenticacaoFilter.ATRIBUTO_USUARIO) Long usuarioId) {
        return alertaService.listar(usuarioId, resolvido);
    }

    @PutMapping("/{id}/resolver")
    @Operation(summary = "Marca o alerta como resolvido")
    public AlertaResponse resolver(@PathVariable Long id,
                                   @Parameter(hidden = true) @RequestAttribute(AutenticacaoFilter.ATRIBUTO_USUARIO) Long usuarioId) {
        return alertaService.resolver(id, usuarioId);
    }
}
