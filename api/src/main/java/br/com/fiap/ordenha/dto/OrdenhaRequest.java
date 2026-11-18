package br.com.fiap.ordenha.dto;

import br.com.fiap.ordenha.model.Turno;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/** Leitura enviada pelos sensores (ou pelo script que simula os sensores). */
public record OrdenhaRequest(
        @NotNull Long vacaId,
        @NotNull @PastOrPresent(message = "não pode ser no futuro") LocalDateTime dataHora,
        @NotNull Turno turno,
        @NotNull @Positive(message = "deve ser maior que zero") @DecimalMax("999.99") Double litros,
        @NotNull @Positive(message = "deve ser maior que zero") @DecimalMax("999.99") Double condutividade) {
}
