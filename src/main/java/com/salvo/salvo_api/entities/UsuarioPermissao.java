package com.salvo.salvo_api.entities;

import com.salvo.salvo_api.domain.usuario.ModuloPermissao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "usuario_permissoes")
@Getter
@Setter
public class UsuarioPermissao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModuloPermissao modulo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    private void prePersist() {
        criadoEm = OffsetDateTime.now();
    }
}
