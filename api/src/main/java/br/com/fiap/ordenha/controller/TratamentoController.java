package br.com.fiap.ordenha.controller;

import br.com.fiap.ordenha.dto.TratamentoRequest;
import br.com.fiap.ordenha.dto.TratamentoResponse;
import br.com.fiap.ordenha.security.AutenticacaoFilter;
import br.com.fiap.ordenha.service.TratamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tratamentos")
@Tag(name = "Tratamentos")
public class TratamentoController {

    private final TratamentoService tratamentoService;

    public TratamentoController(TratamentoService tratamentoService) {
        this.tratamentoService = tratamentoService;
    }

    @PostMapping
    @Operation(summary = "Registra tratamento e muda o status da vaca para EM_TRATAMENTO")
    public ResponseEntity<TratamentoResponse> registrar(@Valid @RequestBody TratamentoRequest req,
                                                        @Parameter(hidden = true) @RequestAttribute(AutenticacaoFilter.ATRIBUTO_USUARIO) Long usuarioId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tratamentoService.registrar(req, usuarioId));
    }
}
