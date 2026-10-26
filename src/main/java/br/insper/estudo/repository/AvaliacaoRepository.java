package br.insper.estudo.repository;

import br.insper.estudo.entity.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * CAMADA REPOSITORY: único lugar que conversa com o banco.
 * JpaRepository já traz save, findById, findAll, deleteById, count...
 * Métodos extras: o Spring Data GERA o SQL a partir do NOME do método.
 *   findByDeletadoFalse                       -> WHERE deletado = false
 *   findByDeletadoFalseAndAutorStartingWith   -> WHERE deletado = false AND autor LIKE 'x%'
 */
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    List<Avaliacao> findByDeletadoFalse();

    List<Avaliacao> findByDeletadoFalseAndAutorStartingWith(String prefixo);

    Optional<Avaliacao> findByIdAndDeletadoFalse(Long id);
}
