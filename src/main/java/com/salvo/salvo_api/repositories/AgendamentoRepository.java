package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.entities.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    List<Agendamento> findByOficinaIdOrderByInicioAsc(Long oficinaId);

    List<Agendamento> findByOficinaIdAndStatusOrderByInicioAsc(Long oficinaId, String status);

    Optional<Agendamento> findByIdAndOficinaId(Long id, Long oficinaId);
}
