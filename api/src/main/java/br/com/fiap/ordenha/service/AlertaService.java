package br.com.fiap.ordenha.service;

import br.com.fiap.ordenha.dto.AlertaResponse;
import br.com.fiap.ordenha.exception.RecursoNaoEncontradoException;
import br.com.fiap.ordenha.model.Alerta;
import br.com.fiap.ordenha.repository.AlertaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AlertaService {

    private final AlertaRepository alertaRepository;

    public AlertaService(AlertaRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    /** Alertas das vacas do usuário, do mais recente ao mais antigo; resolvido nulo traz todos. */
    @Transactional(readOnly = true)
    public List<AlertaResponse> listar(Long usuarioId, Boolean resolvido) {
        List<Alerta> alertas = resolvido == null
                ? alertaRepository.findByVacaPropriedadeUsuarioIdOrderByDataHoraDesc(usuarioId)
                : alertaRepository.findByVacaPropriedadeUsuarioIdAndResolvidoOrderByDataHoraDesc(usuarioId, resolvido);
        return alertas.stream().map(AlertaResponse::de).toList();
    }

    @Transactional
    public AlertaResponse resolver(Long alertaId, Long usuarioId) {
        Alerta alerta = alertaRepository.findByIdAndVacaPropriedadeUsuarioId(alertaId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alerta " + alertaId + " não encontrado"));
        alerta.setResolvido(true);
        return AlertaResponse.de(alerta);
    }
}
