package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.domain.usuario.ModuloPermissao;
import com.salvo.salvo_api.domain.usuario.PerfilUsuario;
import com.salvo.salvo_api.entities.Usuario;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Dados de um integrante da equipe para a tela administrativa.
 * Não inclui dados da oficina porque a listagem já é sempre da oficina logada.
 */
public record UsuarioDTO(
    Long id,
    String nome,
    String email,
    String cargo,
    PerfilUsuario perfil,
    String telefone,
    String especialidades,
    Boolean lider,
    BigDecimal comissaoPercentual,
    String corAgenda,
    String fotoUrl,
    Boolean ativo,
    Boolean executaOs,
    Boolean deveTrocarSenha,
    Boolean dono,
    List<ModuloPermissao> permissoes,
    OffsetDateTime criadoEm
) {
    public static UsuarioDTO from(Usuario u) {
        return from(u, List.of());
    }

    /** Versão para quem não é administrador: sem comissão, permissões, e-mail e telefone. */
    public UsuarioDTO semDadosSensiveis() {
        return new UsuarioDTO(id, nome, null, cargo, perfil, null, especialidades, lider,
            null, corAgenda, fotoUrl, ativo, executaOs, deveTrocarSenha, dono, List.of(), criadoEm);
    }

    public static UsuarioDTO from(Usuario u, List<ModuloPermissao> permissoes) {
        return new UsuarioDTO(
            u.getId(),
            u.getNome(),
            u.getEmail(),
            u.getCargo(),
            u.getPerfil(),
            u.getTelefone(),
            u.getEspecialidades(),
            u.getLider(),
            u.getComissaoPercentual(),
            u.getCorAgenda(),
            u.getFotoUrl(),
            u.getAtivo(),
            u.getExecutaOs(),
            u.getDeveTrocarSenha(),
            u.getDono(),
            permissoes == null ? List.of() : List.copyOf(permissoes),
            u.getCriadoEm()
        );
    }
}
