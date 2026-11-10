package com.salvo.salvo_api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "pedido_compra_itens")
@Getter
@Setter
public class PedidoCompraItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_compra_id", nullable = false)
    private PedidoCompra pedido;

    @Column(name = "peca_id")
    private Long pecaId;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private BigDecimal quantidade;

    @Column(name = "custo_unitario", nullable = false)
    private BigDecimal custoUnitario = BigDecimal.ZERO;

    @Column(name = "quantidade_recebida", nullable = false)
    private BigDecimal quantidadeRecebida = BigDecimal.ZERO;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    private void prePersist() {
        criadoEm = OffsetDateTime.now();
    }
}
