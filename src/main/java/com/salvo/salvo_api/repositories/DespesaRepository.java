package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.entities.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {

    List<Despesa> findByOficinaIdOrderByCriadoEmDesc(Long oficinaId);

    List<Despesa> findByOficinaIdAndStatusOrderByCriadoEmDesc(Long oficinaId, String status);

    Optional<Despesa> findByIdAndOficinaId(Long id, Long oficinaId);
}
