package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.entities.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {
    List<Orcamento> findByOficinaIdOrderByNumeroDesc(Long oficinaId);
    Optional<Orcamento> findByIdAndOficinaId(Long id, Long oficinaId);
    Optional<Orcamento> findByOficinaIdAndNumero(Long oficinaId, Integer numero);

    @Query("select coalesce(max(o.numero), 0) from Orcamento o where o.oficina.id = :oficinaId")
    Integer maxNumero(@Param("oficinaId") Long oficinaId);
}
