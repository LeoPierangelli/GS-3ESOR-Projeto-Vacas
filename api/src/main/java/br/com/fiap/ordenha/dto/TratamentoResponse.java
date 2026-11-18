package br.com.fiap.ordenha.dto;

import br.com.fiap.ordenha.model.Tratamento;

import java.time.LocalDate;

public record TratamentoResponse(Long id, Long vacaId, String medicamento, LocalDate dataInicio, LocalDate dataFim,
                                 Integer diasCarencia, LocalDate dataLiberacao, String observacao) {

    public static TratamentoResponse de(Tratamento t) {
        return new TratamentoResponse(t.getId(), t.getVaca().getId(), t.getMedicamento(), t.getDataInicio(),
                t.getDataFim(), t.getDiasCarencia(), t.getDataLiberacao(), t.getObservacao());
    }
}
