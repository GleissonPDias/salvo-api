package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.entities.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByOficinaIdOrderByCriadoEmDesc(Long oficinaId);

    List<Notificacao> findByOficinaIdAndLidaFalseOrderByCriadoEmDesc(Long oficinaId);

    Optional<Notificacao> findByIdAndOficinaId(Long id, Long oficinaId);

    long countByOficinaIdAndLidaFalse(Long oficinaId);
}
