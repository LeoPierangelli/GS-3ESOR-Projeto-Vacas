package br.com.fiap.ordenha.dto;

import br.com.fiap.ordenha.model.StatusVaca;
import br.com.fiap.ordenha.model.Vaca;

import java.time.LocalDate;

public record VacaResponse(Long id, Long propriedadeId, String brinco, String nome, LocalDate dataNascimento,
                           StatusVaca status, long alertasAbertos) {

    public static VacaResponse de(Vaca v, long alertasAbertos) {
        return new VacaResponse(v.getId(), v.getPropriedade().getId(), v.getBrinco(), v.getNome(),
                v.getDataNascimento(), v.getStatus(), alertasAbertos);
    }
}
