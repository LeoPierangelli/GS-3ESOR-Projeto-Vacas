package br.com.fiap.ordenha.service;

import br.com.fiap.ordenha.dto.TratamentoRequest;
import br.com.fiap.ordenha.dto.TratamentoResponse;
import br.com.fiap.ordenha.model.StatusVaca;
import br.com.fiap.ordenha.model.Tratamento;
import br.com.fiap.ordenha.model.Vaca;
import br.com.fiap.ordenha.repository.TratamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TratamentoService {

    private final VacaService vacaService;
    private final TratamentoRepository tratamentoRepository;

    public TratamentoService(VacaService vacaService, TratamentoRepository tratamentoRepository) {
        this.vacaService = vacaService;
        this.tratamentoRepository = tratamentoRepository;
    }

    /** Registra o tratamento e passa a vaca para EM_TRATAMENTO. */
    @Transactional
    public TratamentoResponse registrar(TratamentoRequest req, Long usuarioId) {
        Vaca vaca = vacaService.buscarDoUsuario(req.vacaId(), usuarioId);
        Tratamento t = new Tratamento();
        t.setVaca(vaca);
        t.setMedicamento(req.medicamento().trim());
        t.setDataInicio(req.dataInicio());
        t.setDataFim(req.dataFim());
        t.setDiasCarencia(req.diasCarencia());
        t.setObservacao(req.observacao());
        tratamentoRepository.save(t);
        vaca.setStatus(StatusVaca.EM_TRATAMENTO);
        return TratamentoResponse.de(t);
    }
}
