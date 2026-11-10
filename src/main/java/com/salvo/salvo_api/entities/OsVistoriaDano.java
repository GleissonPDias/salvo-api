package com.salvo.salvo_api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "os_vistoria_danos")
@Getter
@Setter
public class OsVistoriaDano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vistoria_id", nullable = false)
    private OsVistoria vistoria;

    @Column(nullable = false)
    private String area;

    @Column(nullable = false)
    private String tipo;

    @Column(columnDefinition = "text")
    private String observacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    private void prePersist() {
        criadoEm = OffsetDateTime.now();
    }
}
