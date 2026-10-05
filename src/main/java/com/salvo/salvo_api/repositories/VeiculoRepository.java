package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.entities.Veiculo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    @EntityGraph(attributePaths = {"cliente"})
    List<Veiculo> findByClienteIdAndAtivoTrue(Long clienteId);

    @EntityGraph(attributePaths = {"cliente"})
    @Query("SELECT v FROM Veiculo v JOIN v.cliente c WHERE v.id = :id AND c.oficina.id = :oficina AND v.ativo = true")
    Optional<Veiculo> findByIdAndOficina(Long id, Long oficina);

    @EntityGraph(attributePaths = {"cliente"})
    @Query(
        value = "SELECT v FROM Veiculo v JOIN v.cliente c WHERE c.oficina.id = :oficina AND v.ativo = true",
        countQuery = "SELECT count(v) FROM Veiculo v JOIN v.cliente c WHERE c.oficina.id = :oficina AND v.ativo = true"
    )
    Page<Veiculo> findByOficina(Long oficina, Pageable pageable);
}
