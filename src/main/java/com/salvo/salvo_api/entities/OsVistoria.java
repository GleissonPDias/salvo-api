package com.salvo.salvo_api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "os_vistorias")
@Getter
@Setter
public class OsVistoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private OrdemServico ordemServico;

    @Column(name = "cliente_ciente", nullable = false)
    private Boolean clienteCiente = false;

    @Column(columnDefinition = "text")
    private String pertences;

    @Column(columnDefinition = "text")
    private String observacoes;

    @Column(name = "realizada_em")
    private OffsetDateTime realizadaEm;

    @Column(name = "realizada_por")
    private Long realizadaPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @OneToMany(mappedBy = "vistoria", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OsVistoriaDano> danos = new ArrayList<>();

    @OneToMany(mappedBy = "vistoria", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OsVistoriaFoto> fotos = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        criadoEm = OffsetDateTime.now();
        atualizadoEm = OffsetDateTime.now();
    }
}
