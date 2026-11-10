package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.NotBlank;

public record CriarNotificacaoDTO(
    Long usuarioId,
    String tipo,
    @NotBlank(message = "Título é obrigatório") String titulo,
    String mensagem,
    String link
) {}
