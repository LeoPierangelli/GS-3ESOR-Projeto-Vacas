package br.com.fiap.ordenha.dto;

import java.time.LocalDateTime;
import java.util.List;

public record LoginResponse(String token, LocalDateTime expiraEm, UsuarioResponse usuario,
                            List<PropriedadeResponse> propriedades) {
}
