package br.com.fiap.ordenha.dto;

import br.com.fiap.ordenha.model.Alerta;
import br.com.fiap.ordenha.model.TipoAlerta;

import java.time.LocalDateTime;

public record AlertaResponse(Long id, Long vacaId, String brinco, String nomeVaca, Long ordenhaId, TipoAlerta tipo,
                             String mensagem, LocalDateTime dataHora, boolean resolvido) {

    public static AlertaResponse de(Alerta a) {
        return new AlertaResponse(a.getId(), a.getVaca().getId(), a.getVaca().getBrinco(), a.getVaca().getNome(),
                a.getOrdenha().getId(), a.getTipo(), a.getMensagem(), a.getDataHora(), a.isResolvido());
    }
}
