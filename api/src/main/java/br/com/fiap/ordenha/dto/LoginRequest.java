package br.com.fiap.ordenha.dto;

import jakarta.validation.constraints.NotBlank;

/** Sem @Email de propósito: qualquer email/senha que não confira responde 401 genérico. */
public record LoginRequest(@NotBlank String email, @NotBlank String senha) {
}
