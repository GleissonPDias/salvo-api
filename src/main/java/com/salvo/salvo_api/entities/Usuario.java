package com.salvo.salvo_api.entities;

import com.salvo.salvo_api.domain.usuario.PerfilUsuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    @Column(name = "auth_user_id", unique = true)
    private UUID authUserId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String cargo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PerfilUsuario perfil = PerfilUsuario.FUNCIONARIO;

    @Column(name = "executa_os", nullable = false)
    private Boolean executaOs = true;

    @Column(name = "deve_trocar_senha", nullable = false)
    private Boolean deveTrocarSenha = false;

    private String telefone;

    private String especialidades;

    @Column(nullable = false)
    private Boolean lider = false;

    @Column(name = "comissao_percentual", nullable = false)
    private java.math.BigDecimal comissaoPercentual = java.math.BigDecimal.ZERO;

    @Column(name = "cor_agenda", length = 7)
    private String corAgenda;

    @Column(name = "foto_url", columnDefinition = "text")
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

    @PreUpdate
    private void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
    }
}
