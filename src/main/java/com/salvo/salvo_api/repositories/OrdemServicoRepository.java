package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.entities.OrdemServico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {

    @EntityGraph(attributePaths = {"cliente", "veiculo", "status", "servicos", "pecas"})
    Optional<OrdemServico> findByOficinaIdAndNumero(Long oficinaId, Integer numero);

    @EntityGraph(attributePaths = {"cliente", "veiculo", "status"})
    Page<OrdemServico> findByOficinaIdOrderByNumeroDesc(Long oficinaId, Pageable pageable);

    @EntityGraph(attributePaths = {"cliente", "veiculo", "status"})
    @Query(
        value = "SELECT os FROM OrdemServico os WHERE os.oficina.id = :oficina AND os.status.nome = :status ORDER BY os.numero DESC",
        countQuery = "SELECT count(os) FROM OrdemServico os WHERE os.oficina.id = :oficina AND os.status.nome = :status"
    )
    Page<OrdemServico> findByOficinaAndStatus(Long oficina, String status, Pageable pageable);

    @EntityGraph(attributePaths = {"cliente", "veiculo", "status"})
    @Query(
        value = "SELECT os FROM OrdemServico os WHERE os.oficina.id = :oficina AND os.cliente.id = :cliente ORDER BY os.numero DESC",
        countQuery = "SELECT count(os) FROM OrdemServico os WHERE os.oficina.id = :oficina AND os.cliente.id = :cliente"
    )
    Page<OrdemServico> findByOficinaAndCliente(Long oficina, Long cliente, Pageable pageable);
}
