package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CriarOrcamentoItemDTO(
    @NotBlank(message = "Tipo do item é obrigatório")
    String tipo,
    Long pecaId,
    Long servicoId,
    String codigo,
    @NotBlank(message = "Descrição é obrigatória")
    String descricao,
    @NotNull(message = "Quantidade é obrigatória")
    @Positive(message = "Quantidade deve ser maior que zero")
    BigDecimal quantidade,
    @NotNull(message = "Valor unitário é obrigatório")
    BigDecimal valorUnitario,
    BigDecimal tempoEstimadoHoras
) {}
