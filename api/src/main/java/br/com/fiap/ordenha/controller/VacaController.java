package br.com.fiap.ordenha.controller;

import br.com.fiap.ordenha.dto.FichaVacaResponse;
import br.com.fiap.ordenha.dto.OrdenhaResponse;
import br.com.fiap.ordenha.dto.VacaRequest;
import br.com.fiap.ordenha.dto.VacaResponse;
import br.com.fiap.ordenha.security.AutenticacaoFilter;
import br.com.fiap.ordenha.service.VacaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/vacas")
@Tag(name = "Vacas")
public class VacaController {

    private final VacaService vacaService;

    public VacaController(VacaService vacaService) {
        this.vacaService = vacaService;
    }

    @GetMapping
    @Operation(summary = "Lista o rebanho do usuário com status e quantidade de alertas abertos")
    public List<VacaResponse> listar(@Parameter(hidden = true) @RequestAttribute(AutenticacaoFilter.ATRIBUTO_USUARIO) Long usuarioId) {
        return vacaService.listar(usuarioId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ficha da vaca: últimas ordenhas, condutividade contra a média, alertas e tratamentos")
    public FichaVacaResponse ficha(@PathVariable Long id,
                                   @Parameter(hidden = true) @RequestAttribute(AutenticacaoFilter.ATRIBUTO_USUARIO) Long usuarioId) {
        return vacaService.ficha(id, usuarioId);
    }

    @PostMapping
    @Operation(summary = "Cadastra vaca numa propriedade do usuário (brinco único na propriedade)")
    public ResponseEntity<VacaResponse> cadastrar(@Valid @RequestBody VacaRequest req,
                                                  @Parameter(hidden = true) @RequestAttribute(AutenticacaoFilter.ATRIBUTO_USUARIO) Long usuarioId) {
        VacaResponse criada = vacaService.cadastrar(req, usuarioId);
        var local = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(local).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados da vaca")
    public VacaResponse atualizar(@PathVariable Long id, @Valid @RequestBody VacaRequest req,
                                  @Parameter(hidden = true) @RequestAttribute(AutenticacaoFilter.ATRIBUTO_USUARIO) Long usuarioId) {
        return vacaService.atualizar(id, req, usuarioId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a vaca e o histórico dela")
    public ResponseEntity<Void> remover(@PathVariable Long id,
                                        @Parameter(hidden = true) @RequestAttribute(AutenticacaoFilter.ATRIBUTO_USUARIO) Long usuarioId) {
        vacaService.remover(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/ordenhas")
    @Operation(summary = "Histórico de ordenhas da vaca, da mais recente para a mais antiga")
    public List<OrdenhaResponse> ordenhas(@PathVariable Long id,
                                          @Parameter(hidden = true) @RequestAttribute(AutenticacaoFilter.ATRIBUTO_USUARIO) Long usuarioId) {
        return vacaService.historicoOrdenhas(id, usuarioId);
    }
}
