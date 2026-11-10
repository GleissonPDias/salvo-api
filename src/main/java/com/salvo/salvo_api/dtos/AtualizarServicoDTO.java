package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record AtualizarServicoDTO(
    String codigo,
    String nome,
    String descricao,
    String categoria,
    String detalhe,
    @PositiveOrZero(message = "Tempo estimado não pode ser negativo") BigDecimal tempoEstimadoHoras,
    String garantiaTipo,
    @PositiveOrZero(message = "Preço base não pode ser negativo") BigDecimal precoBase,
    Boolean ativo
) {}
