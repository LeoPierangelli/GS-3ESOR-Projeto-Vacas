package br.com.fiap.ordenha.repository;

import br.com.fiap.ordenha.model.Tratamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TratamentoRepository extends JpaRepository<Tratamento, Long> {

    List<Tratamento> findByVacaIdOrderByDataInicioDesc(Long vacaId);
}
