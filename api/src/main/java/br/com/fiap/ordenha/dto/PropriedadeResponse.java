package br.com.fiap.ordenha.dto;

import br.com.fiap.ordenha.model.Propriedade;

public record PropriedadeResponse(Long id, String nome, String municipio, String uf) {

    public static PropriedadeResponse de(Propriedade p) {
        return new PropriedadeResponse(p.getId(), p.getNome(), p.getMunicipio(), p.getUf());
    }
}
