package com.salvo.salvo_api.dtos;

/**
 * Senha temporária gerada pelo administrador para redefinição.
 * Deve ser exibida uma única vez.
 */
public record RedefinirSenhaResponseDTO(
    String senhaTemporaria
) {}
