package com.salvo.salvo_api.entities;

import com.salvo.salvo_api.domain.os.PrioridadeOs;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ordens_servico")
@Getter
@Setter
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // NUNCA muda após criação — reforçado também por trigger no banco (V3)
    @Column(nullable = false, updatable = false)
    private Integer numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    // box_id é opcional (a OS pode não estar em nenhum box)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_id")
    private Box box;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private StatusOrdemServico status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_abertura_id")
    private Usuario usuarioAbertura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mecanico_lider_id")
    private Usuario mecanicoLider;

    // columnDefinition = "prioridade_os" é necessário porque o banco usa
    // um enum nativo do PostgreSQL, não um varchar.
    // @Enumerated(STRING) diz ao Hibernate para gravar/ler como String,
    // e o driver JDBC do PostgreSQL faz a conversão com o tipo nativo.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private PrioridadeOs prioridade = PrioridadeOs.NORMAL;

    @Column(name = "km_entrada")
    private Integer kmEntrada;

    @Column(columnDefinition = "text")
    private String diagnostico;

    @Column(columnDefinition = "text")
    private String observacoes;

    private String tipo;

    private String canal;

    private String titulo;

    @Column(name = "relato_cliente", columnDefinition = "text")
    private String relatoCliente;

    @Column(name = "observacoes_internas", columnDefinition = "text")
    private String observacoesInternas;

    @Column(name = "observacoes_cliente", columnDefinition = "text")
    private String observacoesCliente;

    @Column(name = "previsao_entrega")
    private OffsetDateTime previsaoEntrega;

    @Column(name = "nivel_combustivel")
    private Short nivelCombustivel;

    @Column(nullable = false)
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(name = "desconto_tipo", nullable = false)
    private String descontoTipo = "VALOR";

    @Column(name = "km_saida")
    private Integer kmSaida;

    @Column(name = "diagnostico_validado", nullable = false)
    private Boolean diagnosticoValidado = false;

    @Column(name = "garantia_dias")
    private Integer garantiaDias;

    @Column(name = "garantia_km")
    private Integer garantiaKm;

    @Column(name = "garantia_tipo")
    private String garantiaTipo;

    @Column(name = "forma_pagamento")
    private String formaPagamento;

    @Column(nullable = false)
    private Short parcelas = 1;

    @Column(name = "aprovacao_status")
    private String aprovacaoStatus;

    @Column(name = "aprovacao_canal")
    private String aprovacaoCanal;

    @Column(name = "aprovacao_enviada_em")
    private OffsetDateTime aprovacaoEnviadaEm;

    @Column(name = "aprovacao_decidida_em")
    private OffsetDateTime aprovacaoDecididaEm;

    @Column(name = "aprovacao_visualizada_em")
    private OffsetDateTime aprovacaoVisualizadaEm;

    @Column(name = "aberta_em", nullable = false, updatable = false)
    private OffsetDateTime abertaEm;

    @Column(name = "pronta_em")
    private OffsetDateTime prontaEm;

    @Column(name = "entregue_em")
    private OffsetDateTime entregueEm;

    @Column(name = "cancelada_em")
    private OffsetDateTime canceladaEm;

    @Column(name = "motivo_cancelamento", columnDefinition = "text")
    private String motivoCancelamento;

    @Column(name = "fechada_em")
    private OffsetDateTime fechadaEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atualizado_por")
    private Usuario atualizadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OsServico> servicos = new ArrayList<>();

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OsPeca> pecas = new ArrayList<>();

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OsHistorico> historico = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        abertaEm = OffsetDateTime.now();
        criadoEm = OffsetDateTime.now();
        atualizadoEm = OffsetDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
    }
}
