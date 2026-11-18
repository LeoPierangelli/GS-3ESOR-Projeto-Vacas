package br.com.fiap.ordenha.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record CadastroRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = Validacao.SEM_HTML, message = Validacao.SEM_HTML_MSG) String nome,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres") String senha,
        @NotNull @Valid PropriedadeRequest propriedade) {
}
