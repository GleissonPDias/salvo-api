package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.entities.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    List<Pagamento> findByOficinaIdOrderByCriadoEmDesc(Long oficinaId);

    List<Pagamento> findByOficinaIdAndStatusOrderByCriadoEmDesc(Long oficinaId, String status);

    Optional<Pagamento> findByIdAndOficinaId(Long id, Long oficinaId);
}
