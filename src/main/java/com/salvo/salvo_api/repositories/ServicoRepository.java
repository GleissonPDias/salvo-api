package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.entities.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    @Query("SELECT s FROM Servico s WHERE s.oficina.id = :oficina AND s.ativo = true " +
        "AND LOWER(s.nome) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Servico> buscar(Long oficina, String q);

    Optional<Servico> findByIdAndOficinaId(Long id, Long oficinaId);
}
