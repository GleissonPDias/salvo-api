package com.salvo.salvo_api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "os_vistoria_fotos")
@Getter
@Setter
public class OsVistoriaFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vistoria_id", nullable = false)
    private OsVistoria vistoria;

    @Column(nullable = false, columnDefinition = "text")
    private String url;

    @Column(name = "storage_path", columnDefinition = "text")
    private String storagePath;

    private String legenda;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    private void prePersist() {
        criadoEm = OffsetDateTime.now();
    }
}
