package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.entities.Peca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PecaRepository extends JpaRepository<Peca, Long> {

    @Query("SELECT p FROM Peca p WHERE p.oficina.id = :oficina " +
        "AND (LOWER(p.nome) LIKE LOWER(CONCAT('%', :q, '%')) " +
        "OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :q, '%'))) " +
        "ORDER BY p.codigo")
    List<Peca> buscar(Long oficina, String q);

    List<Peca> findByOficinaIdOrderByCodigo(Long oficinaId);

    List<Peca> findByOficinaIdAndIdInOrderByCodigo(Long oficinaId, java.util.Collection<Long> ids);

    Optional<Peca> findByIdAndOficinaId(Long id, Long oficinaId);
}
