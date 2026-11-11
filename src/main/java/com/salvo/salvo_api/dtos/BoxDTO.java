package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.Box;

public record BoxDTO(
    Long id,
    String nome,
    String descricao,
    Boolean ativo
) {
    public static BoxDTO from(Box box) {
        return new BoxDTO(box.getId(), box.getNome(), box.getDescricao(), box.getAtivo());
    }
}
