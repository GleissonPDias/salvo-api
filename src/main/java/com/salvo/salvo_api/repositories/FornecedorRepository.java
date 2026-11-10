package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.entities.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    List<Fornecedor> findByOficinaIdOrderByNome(Long oficinaId);

    List<Fornecedor> findByOficinaIdAndAtivoTrueOrderByNome(Long oficinaId);

    Optional<Fornecedor> findByIdAndOficinaId(Long id, Long oficinaId);
}
