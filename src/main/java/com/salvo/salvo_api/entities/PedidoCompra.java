package com.salvo.salvo_api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "pedidos_compra")
@Getter
@Setter
public class PedidoCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    @Column(nullable = false, updatable = false)
    private Integer numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @Column(nullable = false)
    private String status = "RASCUNHO";

    @Column(name = "data_pedido", nullable = false)
    private java.time.LocalDate dataPedido;

    @Column(name = "previsao_entrega")
    private java.time.LocalDate previsaoEntrega;

    @Column(name = "recebido_em")
    private OffsetDateTime recebidoEm;

    @Column(name = "nota_fiscal")
    private String notaFiscal;

    @Column(columnDefinition = "text")
    private String observacoes;

    @Column(name = "criado_por")
    private Long criadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<PedidoCompraItem> itens = new java.util.ArrayList<>();

    @PrePersist
    private void prePersist() {
        criadoEm = OffsetDateTime.now();
        atualizadoEm = OffsetDateTime.now();
    }

    public BigDecimal total() {
        return itens.stream()
            .map(item -> item.getQuantidade().multiply(item.getCustoUnitario()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
