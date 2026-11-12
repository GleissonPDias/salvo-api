package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrocarUsuarioDTO(
    @NotNull(message = "Informe o usuário para entrar")
    Long usuarioId,
    @NotBlank(message = "A senha do usuário é obrigatória")
    String senha
) {}
