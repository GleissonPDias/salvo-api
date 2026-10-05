package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.entities.StatusOrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatusOrdemServicoRepository extends JpaRepository<StatusOrdemServico, Short> {
    Optional<StatusOrdemServico> findByNome(String nome);
}
