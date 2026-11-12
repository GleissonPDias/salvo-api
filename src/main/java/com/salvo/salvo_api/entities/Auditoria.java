package com.salvo.salvo_api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Trilha de auditoria das ações de escrita feitas no app.
 *
 * O nome do usuário é copiado no momento da ação para que o histórico
 * continue legível mesmo após renomear ou remover o usuário.
 */
@Entity
@Table(name = "auditoria")
@Getter
@Setter
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oficina_id", nullable = false)
    private Long oficinaId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_nome", nullable = false)
    private String usuarioNome;

    @Column(nullable = false, length = 30)
    private String acao;

    @Column(length = 30)
    private String modulo;

    @Column(length = 60)
    private String entidade;

    @Column(name = "entidade_id", length = 60)
    private String entidadeId;

    @Column(nullable = false, length = 10)
    private String metodo;

    @Column(nullable = false, length = 300)
    private String caminho;

    @Column(name = "status_http")
    private Integer statusHttp;

    @Column(columnDefinition = "text")
    private String resumo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    private void prePersist() {
        criadoEm = OffsetDateTime.now();
    }
}
