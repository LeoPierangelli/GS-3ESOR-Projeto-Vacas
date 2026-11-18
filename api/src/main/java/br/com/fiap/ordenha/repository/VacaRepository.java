package br.com.fiap.ordenha.repository;

import br.com.fiap.ordenha.model.Vaca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Todas as buscas filtram pelo dono da propriedade: é aqui que o controle de acesso
 * "o usuário só enxerga vacas da própria propriedade" chega ao banco.
 */
public interface VacaRepository extends JpaRepository<Vaca, Long> {

    List<Vaca> findByPropriedadeUsuarioIdOrderByBrinco(Long usuarioId);

    Optional<Vaca> findByIdAndPropriedadeUsuarioId(Long id, Long usuarioId);

    boolean existsByPropriedadeIdAndBrinco(Long propriedadeId, String brinco);

    boolean existsByPropriedadeIdAndBrincoAndIdNot(Long propriedadeId, String brinco, Long id);
}
