package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.Size;

public record AtualizarBoxDTO(
    @Size(max = 60, message = "Nome do box deve ter até 60 caracteres")
    String nome,

    @Size(max = 200, message = "Descrição deve ter até 200 caracteres")
    String descricao,

    Boolean ativo
) {}
