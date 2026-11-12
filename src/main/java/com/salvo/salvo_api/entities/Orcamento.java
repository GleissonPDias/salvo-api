package com.salvo.salvo_api.entities;

import com.salvo.salvo_api.domain.orcamento.StatusOrcamento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orcamentos")
@Getter
@Setter
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    @Column(nullable = false)
    private Integer numero;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "veiculo_id", nullable = false)
    private Long veiculoId;

    @Column(name = "criado_por")
    private Long criadoPor;

    @Column(name = "ordem_servico_id")
    private Long ordemServicoId;

    @Column(nullable = false, columnDefinition = "status_orcamento")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private StatusOrcamento status = StatusOrcamento.RASCUNHO;

    @Column
    private LocalDate validade;

    @Column(columnDefinition = "text")
    private String observacoes;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(name = "desconto_tipo", nullable = false, length = 20)
    private String descontoTipo = "valor";

    @Column(length = 120)
    private String titulo;

    @Column(length = 40)
    private String tipo;

    @Column(name = "validade_dias", nullable = false)
    private Short validadeDias = 7;

    @Column(name = "canal_envio", length = 20)
    private String canalEnvio;

    @Column(name = "emitido_em")
    private OffsetDateTime emitidoEm;

    @Column(name = "aprovado_em")
    private OffsetDateTime aprovadoEm;

    @Column(name = "rejeitado_em")
    private OffsetDateTime rejeitadoEm;

    @Column(name = "motivo_rejeicao", columnDefinition = "text")
    private String motivoRejeicao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrcamentoItem> itens = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (criadoEm == null) criadoEm = now;
        if (atualizadoEm == null) atualizadoEm = now;
    }

    @PreUpdate
    private void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
    }
}
