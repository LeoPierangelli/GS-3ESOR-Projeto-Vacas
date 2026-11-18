package br.com.fiap.ordenha.repository;

import br.com.fiap.ordenha.model.Propriedade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropriedadeRepository extends JpaRepository<Propriedade, Long> {

    List<Propriedade> findByUsuarioIdOrderByNome(Long usuarioId);

    Optional<Propriedade> findByIdAndUsuarioId(Long id, Long usuarioId);
}
