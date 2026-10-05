package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.entities.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Page<Cliente> findByOficinaIdAndAtivoTrue(Long oficinaId, Pageable pageable);

    @Query("SELECT c FROM Cliente c WHERE c.oficina.id = :oficinaId AND c.id = :id AND c.ativo = true")
    Optional<Cliente> findByOficinaIdAndId(Long oficinaId, Long id);

    @Query("SELECT c FROM Cliente c WHERE c.oficina.id = :oficina AND c.ativo = true AND " +
            "(LOWER(c.nome) LIKE LOWER(CONCAT('%', :q, '%')) OR c.cpfCnpj LIKE CONCAT('%', :q, '%'))")
    Page<Cliente> buscar(Long oficina, String q, Pageable pageable);

}
