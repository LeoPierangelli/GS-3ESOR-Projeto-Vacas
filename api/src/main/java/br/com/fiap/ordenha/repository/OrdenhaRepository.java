package br.com.fiap.ordenha.repository;

import br.com.fiap.ordenha.model.Ordenha;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrdenhaRepository extends JpaRepository<Ordenha, Long> {

    List<Ordenha> findByVacaIdOrderByDataHoraDesc(Long vacaId);

    /** A mesma vaca não pode ter duas leituras no mesmo momento (UNIQUE vaca_id, data_hora no banco). */
    boolean existsByVacaIdAndDataHora(Long vacaId, LocalDateTime dataHora);

    List<Ordenha> findByVacaIdOrderByDataHoraDesc(Long vacaId, Pageable pageable);

    /** Histórico anterior a uma leitura, da mais recente para a mais antiga (base das regras de alerta). */
    List<Ordenha> findByVacaIdAndDataHoraBeforeOrderByDataHoraDesc(Long vacaId, LocalDateTime dataHora, Pageable pageable);
}
