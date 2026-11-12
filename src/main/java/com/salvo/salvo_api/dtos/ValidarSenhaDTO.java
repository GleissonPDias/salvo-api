package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ValidarSenhaDTO(
    @NotNull(message = "Informe o usuário")
    Long usuarioId,
    @NotBlank(message = "A senha é obrigatória")
    String senha
) {}
