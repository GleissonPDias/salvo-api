package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record AtualizarOsPecaDTO(
    Long pecaId,
    @NotBlank(message = "Descrição é obrigatória") String descricao,
    @NotNull @Positive(message = "Quantidade deve ser maior que zero") BigDecimal quantidade,
    @NotNull @PositiveOrZero(message = "Valor unitário não pode ser negativo") BigDecimal valorUnitario,
    Boolean cotada,
    Boolean instalada,
    @PositiveOrZero(message = "Desconto não pode ser negativo") BigDecimal desconto,
    String descontoTipo,
    String unidade
) {}
