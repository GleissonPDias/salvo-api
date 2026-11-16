package com.salvo.salvo_api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "orcamento_itens")
@Getter
@Setter
public class OrcamentoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orcamento_id", nullable = false)
    private Orcamento orcamento;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(name = "peca_id")
    private Long pecaId;

    @Column(name = "servico_id")
    private Long servicoId;

    @Column(length = 60)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidade;

    @Column(name = "valor_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorUnitario;

    @Column(name = "tempo_estimado_horas", precision = 8, scale = 2)
    private BigDecimal tempoEstimadoHoras;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    private void prePersist() {
        if (criadoEm == null) criadoEm = OffsetDateTime.now();
    }
}
