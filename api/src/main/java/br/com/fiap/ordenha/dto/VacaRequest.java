package br.com.fiap.ordenha.dto;

import br.com.fiap.ordenha.model.StatusVaca;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** Usado no POST e no PUT de /vacas. Se o status vier vazio, a vaca fica ATIVA. */
public record VacaRequest(
        @NotNull Long propriedadeId,
        @NotBlank @Size(max = 20) @Pattern(regexp = Validacao.SEM_HTML, message = Validacao.SEM_HTML_MSG) String brinco,
        @Size(max = 60) @Pattern(regexp = Validacao.SEM_HTML, message = Validacao.SEM_HTML_MSG) String nome,
        @PastOrPresent LocalDate dataNascimento,
        StatusVaca status) {
}
