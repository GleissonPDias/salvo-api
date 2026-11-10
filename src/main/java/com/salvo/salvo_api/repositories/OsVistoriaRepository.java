package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.entities.OsVistoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OsVistoriaRepository extends JpaRepository<OsVistoria, Long> {

    Optional<OsVistoria> findByOrdemServicoId(Long ordemServicoId);
}
