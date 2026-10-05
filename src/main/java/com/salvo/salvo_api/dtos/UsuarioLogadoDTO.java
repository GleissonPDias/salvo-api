package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.Usuario;

public record UsuarioLogadoDTO(
    Long id,
    Long oficinaId,
    String oficinaNome,
    String nome,
    String email,
    String cargo
) {
    public static UsuarioLogadoDTO from(Usuario u) {
        return new UsuarioLogadoDTO(
            u.getId(),
            u.getOficina().getId(),
            u.getOficina().getNome(),
            u.getNome(),
            u.getEmail(),
            u.getCargo()
        );
    }
}
