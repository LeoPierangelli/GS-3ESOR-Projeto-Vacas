package br.com.fiap.ordenha.repository;

import br.com.fiap.ordenha.model.Alerta;
import br.com.fiap.ordenha.model.TipoAlerta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    List<Alerta> findByVacaPropriedadeUsuarioIdOrderByDataHoraDesc(Long usuarioId);

    List<Alerta> findByVacaPropriedadeUsuarioIdAndResolvidoOrderByDataHoraDesc(Long usuarioId, boolean resolvido);

    Optional<Alerta> findByIdAndVacaPropriedadeUsuarioId(Long id, Long usuarioId);

    List<Alerta> findByVacaIdAndResolvidoFalseOrderByDataHoraDesc(Long vacaId);

    long countByVacaIdAndResolvidoFalse(Long vacaId);

    boolean existsByVacaIdAndTipoAndResolvidoFalse(Long vacaId, TipoAlerta tipo);
}
