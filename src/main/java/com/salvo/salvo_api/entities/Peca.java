package com.salvo.salvo_api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "pecas")
@Getter
@Setter
public class Peca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    private String fabricante;

    private String oem;

    private String categoria;

    @Column(columnDefinition = "text")
    private String aplicacao;

    @Column(nullable = false)
    private String unidade = "un";

    @Column(name = "preco_custo", nullable = false)
    private BigDecimal precoCusto = BigDecimal.ZERO;

    @Column(name = "preco_venda", nullable = false)
    private BigDecimal precoVenda = BigDecimal.ZERO;

    @Column(name = "estoque_atual", nullable = false)
    private BigDecimal estoqueAtual = BigDecimal.ZERO;

    @Column(name = "estoque_minimo", nullable = false)
    private BigDecimal estoqueMinimo = BigDecimal.ZERO;

    @Column(name = "estoque_maximo", nullable = false)
    private BigDecimal estoqueMaximo = BigDecimal.ZERO;

    @Column(name = "estoque_reservado", nullable = false)
    private BigDecimal estoqueReservado = BigDecimal.ZERO;

    @Column(name = "estoque_disponivel", insertable = false, updatable = false)
    private BigDecimal estoqueDisponivel;

    private String localizacao;

    @Column(name = "fornecedor_id")
    private Long fornecedorId;

    @Column(columnDefinition = "text")
    private String observacao;

    @Column(name = "codigo_barras")
    private String codigoBarras;

    @Column(name = "garantia_dias")
    private Integer garantiaDias;

    private String ncm;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @PrePersist
    private void prePersist() {
        criadoEm = OffsetDateTime.now();
        atualizadoEm = OffsetDateTime.now();
    }
}
