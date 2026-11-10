package com.salvo.salvo_api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "agendamentos")
@Getter
@Setter
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "veiculo_id")
    private Long veiculoId;

    @Column(name = "servico_id")
    private Long servicoId;

    @Column(name = "mecanico_id")
    private Long mecanicoId;

    @Column(name = "box_id")
    private Long boxId;

    @Column(name = "ordem_servico_id")
    private Long ordemServicoId;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(nullable = false)
    private OffsetDateTime inicio;

    @Column(nullable = false)
    private OffsetDateTime fim;

    @Column(nullable = false)
    private String status = "AGENDADO";

    @Column(name = "criado_por")
    private Long criadoPor;

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
