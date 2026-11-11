package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.NotNull;

public record AlterarStatusUsuarioDTO(
    @NotNull(message = "Informe se o usuário deve ficar ativo")
    Boolean ativo
) {}
