package br.com.fiap.ordenha.dto;

import br.com.fiap.ordenha.model.Ordenha;
import br.com.fiap.ordenha.model.Turno;

import java.time.LocalDateTime;

public record OrdenhaResponse(Long id, Long vacaId, LocalDateTime dataHora, Turno turno, Double litros,
                              Double condutividade) {

    public static OrdenhaResponse de(Ordenha o) {
        return new OrdenhaResponse(o.getId(), o.getVaca().getId(), o.getDataHora(), o.getTurno(), o.getLitros(),
                o.getCondutividade());
    }
}
