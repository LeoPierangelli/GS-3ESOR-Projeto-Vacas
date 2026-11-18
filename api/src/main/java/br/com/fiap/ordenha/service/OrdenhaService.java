package br.com.fiap.ordenha.service;

import br.com.fiap.ordenha.dto.AlertaResponse;
import br.com.fiap.ordenha.dto.OrdenhaRegistradaResponse;
import br.com.fiap.ordenha.dto.OrdenhaRequest;
import br.com.fiap.ordenha.dto.OrdenhaResponse;
import br.com.fiap.ordenha.model.*;
import br.com.fiap.ordenha.repository.AlertaRepository;
import br.com.fiap.ordenha.repository.OrdenhaRepository;
import br.com.fiap.ordenha.repository.TratamentoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Recebe a leitura dos sensores, grava a ordenha e aplica as regras de alerta no mesmo momento.
 */
@Service
public class OrdenhaService {

    private final VacaService vacaService;
    private final RegrasAlertaService regras;
    private final OrdenhaRepository ordenhaRepository;
    private final AlertaRepository alertaRepository;
    private final TratamentoRepository tratamentoRepository;

    public OrdenhaService(VacaService vacaService, RegrasAlertaService regras, OrdenhaRepository ordenhaRepository,
                          AlertaRepository alertaRepository, TratamentoRepository tratamentoRepository) {
        this.vacaService = vacaService;
        this.regras = regras;
        this.ordenhaRepository = ordenhaRepository;
        this.alertaRepository = alertaRepository;
        this.tratamentoRepository = tratamentoRepository;
    }

    @Transactional
    public OrdenhaRegistradaResponse registrar(OrdenhaRequest req, Long usuarioId) {
        Vaca vaca = vacaService.buscarDoUsuario(req.vacaId(), usuarioId);

        // Histórico ANTES desta leitura, da mais recente para a mais antiga.
        List<Ordenha> historico = ordenhaRepository.findByVacaIdAndDataHoraBeforeOrderByDataHoraDesc(
                vaca.getId(), req.dataHora(), PageRequest.of(0, regras.tamanhoHistoricoNecessario()));
        List<Tratamento> tratamentos = tratamentoRepository.findByVacaIdOrderByDataInicioDesc(vaca.getId());

        Ordenha ordenha = new Ordenha();
        ordenha.setVaca(vaca);
        ordenha.setDataHora(req.dataHora());
        ordenha.setTurno(req.turno());
        ordenha.setLitros(req.litros());
        ordenha.setCondutividade(req.condutividade());
        ordenhaRepository.save(ordenha);

        List<TipoAlerta> tipos = new ArrayList<>();
        if (regras.suspeitaMastite(req.condutividade(), historico.stream().map(Ordenha::getCondutividade).toList())) {
            tipos.add(TipoAlerta.SUSPEITA_MASTITE);
        }
        // Queda é tendência: não repete enquanto já houver um alerta de queda aberto para a vaca.
        if (regras.quedaProducao(req.litros(), historico.stream().map(Ordenha::getLitros).toList())
                && !alertaRepository.existsByVacaIdAndTipoAndResolvidoFalse(vaca.getId(), TipoAlerta.QUEDA_PRODUCAO)) {
            tipos.add(TipoAlerta.QUEDA_PRODUCAO);
        }
        if (regras.leiteEmCarencia(req.dataHora().toLocalDate(), tratamentos)) {
            tipos.add(TipoAlerta.LEITE_EM_CARENCIA);
        }

        List<AlertaResponse> gerados = new ArrayList<>();
        for (TipoAlerta tipo : tipos) {
            Alerta alerta = new Alerta();
            alerta.setVaca(vaca);
            alerta.setOrdenha(ordenha);
            alerta.setTipo(tipo);
            alerta.setMensagem(tipo.getMensagem());
            alerta.setDataHora(req.dataHora());
            alerta.setResolvido(false);
            alertaRepository.save(alerta);
            gerados.add(AlertaResponse.de(alerta));
        }
        return new OrdenhaRegistradaResponse(OrdenhaResponse.de(ordenha), gerados);
    }
}
