package br.com.fiap.ordenha.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/** Formato único de erro da API. "campos" só aparece em erros de validação (400). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(int status, String erro, String mensagem, Map<String, String> campos,
                           LocalDateTime timestamp) {

    public static ErroResponse de(int status, String erro, String mensagem) {
        return new ErroResponse(status, erro, mensagem, null, LocalDateTime.now());
    }
}
