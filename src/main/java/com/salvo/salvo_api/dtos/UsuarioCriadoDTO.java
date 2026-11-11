package com.salvo.salvo_api.dtos;

/**
 * Resposta da criação de funcionário.
 *
 * senhaTemporaria só é preenchida quando o backend gerou a senha.
 * Ela deve ser exibida uma única vez ao administrador.
 */
public record UsuarioCriadoDTO(
    UsuarioDTO usuario,
    String senhaTemporaria
) {}
