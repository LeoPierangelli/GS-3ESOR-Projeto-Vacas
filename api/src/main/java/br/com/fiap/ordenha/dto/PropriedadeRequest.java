package br.com.fiap.ordenha.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PropriedadeRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = Validacao.SEM_HTML, message = Validacao.SEM_HTML_MSG) String nome,
        @NotBlank @Size(max = 100) @Pattern(regexp = Validacao.SEM_HTML, message = Validacao.SEM_HTML_MSG) String municipio,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "deve ter 2 letras") String uf) {
}
