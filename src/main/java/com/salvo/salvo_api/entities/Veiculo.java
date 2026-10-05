package com.salvo.salvo_api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "veiculos")
@Getter
@Setter
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, unique = true)
    private String placa;

    @Column(unique = true)
    private String chassi;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    private String versao;

    @Column(name = "ano_fabricacao")
    private Short anoFabricacao;

    @Column(name = "ano_modelo")
    private Short anoModelo;

    private String cor;

    private String combustivel;

    private String motorizacao;

    private String transmissao;

    @Column(name = "km_atual", nullable = false)
    private Integer kmAtual = 0;

    @Column(name = "foto_url")
    private String fotoUrl;

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
