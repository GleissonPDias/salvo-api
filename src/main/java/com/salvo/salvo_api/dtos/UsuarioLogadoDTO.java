package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.domain.usuario.ModuloPermissao;
import com.salvo.salvo_api.domain.usuario.PerfilUsuario;
import com.salvo.salvo_api.entities.Usuario;

import java.math.BigDecimal;
import java.util.List;

public record UsuarioLogadoDTO(
    Long id,
    Long oficinaId,
    String oficinaNome,
    String nome,
    String email,
    String cargo,
    PerfilUsuario perfil,
    Boolean ativo,
    Boolean executaOs,
    String telefone,
    String especialidades,
    Boolean lider,
    BigDecimal comissaoPercentual,
    String corAgenda,
    String fotoUrl,
    Boolean deveTrocarSenha,
    Boolean dono,
    List<ModuloPermissao> permissoes
) {
    public static UsuarioLogadoDTO from(Usuario u) {
        return from(u, List.of());
    }

    public static UsuarioLogadoDTO from(Usuario u, List<ModuloPermissao> permissoes) {
        return new UsuarioLogadoDTO(
            u.getId(),
            u.getOficina().getId(),
            u.getOficina().getNome(),
            u.getNome(),
            u.getEmail(),
            u.getCargo(),
            u.getPerfil(),
            u.getAtivo(),
            u.getExecutaOs(),
            u.getTelefone(),
            u.getEspecialidades(),
            u.getLider(),
            u.getComissaoPercentual(),
            u.getCorAgenda(),
            u.getFotoUrl(),
            u.getDeveTrocarSenha(),
            u.getDono(),
            permissoes == null ? List.of() : List.copyOf(permissoes)
        );
    }
}
