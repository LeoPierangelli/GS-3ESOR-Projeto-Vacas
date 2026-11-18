package br.com.fiap.ordenha.dto;

import br.com.fiap.ordenha.model.Usuario;

/** Nunca inclui a senha nem o hash. */
public record UsuarioResponse(Long id, String nome, String email) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail());
    }
}
