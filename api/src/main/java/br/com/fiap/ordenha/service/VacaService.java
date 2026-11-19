package br.com.fiap.ordenha.service;

import br.com.fiap.ordenha.dto.*;
import br.com.fiap.ordenha.exception.ConflitoException;
import br.com.fiap.ordenha.exception.RecursoNaoEncontradoException;
import br.com.fiap.ordenha.model.Ordenha;
import br.com.fiap.ordenha.model.Propriedade;
import br.com.fiap.ordenha.model.StatusVaca;
import br.com.fiap.ordenha.model.Vaca;
import br.com.fiap.ordenha.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VacaService {

    private static final int ORDENHAS_NA_FICHA = 14;

    private final VacaRepository vacaRepository;
    private final PropriedadeRepository propriedadeRepository;
    private final OrdenhaRepository ordenhaRepository;
    private final AlertaRepository alertaRepository;
    private final TratamentoRepository tratamentoRepository;

    public VacaService(VacaRepository vacaRepository, PropriedadeRepository propriedadeRepository,
                       OrdenhaRepository ordenhaRepository, AlertaRepository alertaRepository,
                       TratamentoRepository tratamentoRepository) {
        this.vacaRepository = vacaRepository;
        this.propriedadeRepository = propriedadeRepository;
        this.ordenhaRepository = ordenhaRepository;
        this.alertaRepository = alertaRepository;
        this.tratamentoRepository = tratamentoRepository;
    }

    /**
     * Busca a vaca garantindo que ela pertence a uma propriedade do usuário logado.
     * Vaca de outro produtor responde 404, igual a vaca inexistente.
     */
    @Transactional(readOnly = true)
    public Vaca buscarDoUsuario(Long vacaId, Long usuarioId) {
        return vacaRepository.findByIdAndPropriedadeUsuarioId(vacaId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vaca " + vacaId + " não encontrada"));
    }

    @Transactional(readOnly = true)
    public List<VacaResponse> listar(Long usuarioId) {
        return vacaRepository.findByPropriedadeUsuarioIdOrderByBrinco(usuarioId).stream()
                .map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public FichaVacaResponse ficha(Long vacaId, Long usuarioId) {
        Vaca vaca = buscarDoUsuario(vacaId, usuarioId);
        List<Ordenha> ultimas = ordenhaRepository.findByVacaIdOrderByDataHoraDesc(vacaId, PageRequest.of(0, ORDENHAS_NA_FICHA));
        Double ultimaCondutividade = ultimas.isEmpty() ? null : ultimas.get(0).getCondutividade();
        Double mediaCondutividade = media(ultimas.stream().skip(1).limit(7).map(Ordenha::getCondutividade).toList());
        Double mediaLitros = media(ultimas.stream().limit(7).map(Ordenha::getLitros).toList());
        var alertas = alertaRepository.findByVacaIdAndResolvidoFalseOrderByDataHoraDesc(vacaId).stream()
                .map(AlertaResponse::de).toList();
        var tratamentos = tratamentoRepository.findByVacaIdOrderByDataInicioDesc(vacaId).stream()
                .map(TratamentoResponse::de).toList();
        return new FichaVacaResponse(VacaResponse.de(vaca, alertas.size()), ultimaCondutividade, mediaCondutividade,
                mediaLitros, ultimas.stream().map(OrdenhaResponse::de).toList(), alertas, tratamentos);
    }

    @Transactional
    public VacaResponse cadastrar(VacaRequest req, Long usuarioId) {
        Propriedade propriedade = propriedadeDoUsuario(req.propriedadeId(), usuarioId);
        String brinco = req.brinco().trim();
        if (vacaRepository.existsByPropriedadeIdAndBrinco(propriedade.getId(), brinco)) {
            throw new ConflitoException("Já existe uma vaca com o brinco " + brinco + " nesta propriedade");
        }
        Vaca vaca = new Vaca();
        preencher(vaca, req, propriedade, brinco);
        vacaRepository.save(vaca);
        return VacaResponse.de(vaca, 0);
    }

    @Transactional
    public VacaResponse atualizar(Long vacaId, VacaRequest req, Long usuarioId) {
        Vaca vaca = buscarDoUsuario(vacaId, usuarioId);
        Propriedade propriedade = propriedadeDoUsuario(req.propriedadeId(), usuarioId);
        String brinco = req.brinco().trim();
        if (vacaRepository.existsByPropriedadeIdAndBrincoAndIdNot(propriedade.getId(), brinco, vacaId)) {
            throw new ConflitoException("Já existe uma vaca com o brinco " + brinco + " nesta propriedade");
        }
        preencher(vaca, req, propriedade, brinco);
        return paraResponse(vaca);
    }

    /** Remove a vaca; ordenhas, alertas e tratamentos dela saem junto (ON DELETE CASCADE no banco). */
    @Transactional
    public void remover(Long vacaId, Long usuarioId) {
        vacaRepository.delete(buscarDoUsuario(vacaId, usuarioId));
    }

    @Transactional(readOnly = true)
    public List<OrdenhaResponse> historicoOrdenhas(Long vacaId, Long usuarioId) {
        buscarDoUsuario(vacaId, usuarioId);
        return ordenhaRepository.findByVacaIdOrderByDataHoraDesc(vacaId).stream().map(OrdenhaResponse::de).toList();
    }

    private Propriedade propriedadeDoUsuario(Long propriedadeId, Long usuarioId) {
        return propriedadeRepository.findByIdAndUsuarioId(propriedadeId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Propriedade " + propriedadeId + " não encontrada"));
    }

    private static void preencher(Vaca vaca, VacaRequest req, Propriedade propriedade, String brinco) {
        vaca.setPropriedade(propriedade);
        vaca.setBrinco(brinco);
        vaca.setNome(req.nome() == null ? null : req.nome().trim());
        vaca.setDataNascimento(req.dataNascimento());
        vaca.setStatus(req.status() == null ? StatusVaca.ATIVA : req.status());
    }

    private VacaResponse paraResponse(Vaca v) {
        return VacaResponse.de(v, alertaRepository.countByVacaIdAndResolvidoFalse(v.getId()));
    }

    private static Double media(List<Double> valores) {
        if (valores.isEmpty()) {
            return null;
        }
        double m = valores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return Math.round(m * 1000) / 1000.0; // 3 casas: a condutividade vem em mS com 3 decimais
    }
}
