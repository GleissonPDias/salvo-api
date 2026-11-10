package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.Usuario;
import java.math.BigDecimal;

public record UsuarioLogadoDTO(
    Long id,
    Long oficinaId,
    String oficinaNome,
    String nome,
    String email,
    String cargo,
    String telefone,
    String especialidades,
    Boolean lider,
    BigDecimal comissaoPercentual,
    String corAgenda,
    String fotoUrl
) {
    public static UsuarioLogadoDTO from(Usuario u) {
        return new UsuarioLogadoDTO(
            u.getId(),
            u.getOficina().getId(),
            u.getOficina().getNome(),
            u.getNome(),
            u.getEmail(),
            u.getCargo(),
            u.getTelefone(),
            u.getEspecialidades(),
            u.getLider(),
            u.getComissaoPercentual(),
            u.getCorAgenda(),
            u.getFotoUrl()
        );
    }
}
