package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.entities.PedidoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PedidoCompraRepository extends JpaRepository<PedidoCompra, Long> {

    @Query("SELECT DISTINCT p FROM PedidoCompra p LEFT JOIN FETCH p.itens " +
        "WHERE p.oficina.id = :oficina ORDER BY p.numero DESC")
    List<PedidoCompra> listar(@Param("oficina") Long oficina);

    @Query("SELECT DISTINCT p FROM PedidoCompra p LEFT JOIN FETCH p.itens " +
        "WHERE p.oficina.id = :oficina AND p.status = :status ORDER BY p.numero DESC")
    List<PedidoCompra> listarPorStatus(@Param("oficina") Long oficina, @Param("status") String status);

    @Query("SELECT DISTINCT p FROM PedidoCompra p LEFT JOIN FETCH p.itens " +
        "WHERE p.id = :id AND p.oficina.id = :oficina")
    Optional<PedidoCompra> buscar(@Param("id") Long id, @Param("oficina") Long oficina);
}
