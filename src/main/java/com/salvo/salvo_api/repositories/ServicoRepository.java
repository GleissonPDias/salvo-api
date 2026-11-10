package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.entities.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    @Query("SELECT s FROM Servico s WHERE s.oficina.id = :oficina " +
        "AND (LOWER(s.nome) LIKE LOWER(CONCAT('%', :q, '%')) " +
        "OR LOWER(s.codigo) LIKE LOWER(CONCAT('%', :q, '%'))) " +
        "ORDER BY s.codigo")
    List<Servico> buscar(Long oficina, String q);

    List<Servico> findByOficinaIdOrderByCodigo(Long oficinaId);

    Optional<Servico> findByIdAndOficinaId(Long id, Long oficinaId);
}
