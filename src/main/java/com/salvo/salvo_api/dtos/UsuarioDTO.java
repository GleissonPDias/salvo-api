package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.domain.usuario.PerfilUsuario;
import com.salvo.salvo_api.entities.Usuario;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
    OffsetDateTime criadoEm
) {
    public static UsuarioDTO from(Usuario u) {
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
            u.getCriadoEm()
        );
    }
}
