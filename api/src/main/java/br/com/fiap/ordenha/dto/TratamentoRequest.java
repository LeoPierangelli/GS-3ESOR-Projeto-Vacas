package br.com.fiap.ordenha.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record TratamentoRequest(
        @NotNull Long vacaId,
        @NotBlank @Size(max = 100) @Pattern(regexp = Validacao.SEM_HTML, message = Validacao.SEM_HTML_MSG) String medicamento,
        @NotNull LocalDate dataInicio,
        LocalDate dataFim,
        @NotNull @PositiveOrZero @Max(365) Integer diasCarencia,
        @Size(max = 255) @Pattern(regexp = Validacao.SEM_HTML, message = Validacao.SEM_HTML_MSG) String observacao) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "dataFim não pode ser antes de dataInicio")
    public boolean isPeriodoValido() {
        return dataInicio == null || dataFim == null || !dataFim.isBefore(dataInicio);
    }
}
