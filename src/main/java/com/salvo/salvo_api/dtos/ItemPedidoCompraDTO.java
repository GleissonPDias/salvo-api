package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ItemPedidoCompraDTO(
    Long pecaId,
    @NotBlank(message = "Descrição do item é obrigatória") String descricao,
    @NotNull(message = "Quantidade é obrigatória")
    @Positive(message = "Quantidade deve ser maior que zero") BigDecimal quantidade,
    @NotNull(message = "Custo unitário é obrigatório")
    @PositiveOrZero(message = "Custo unitário não pode ser negativo") BigDecimal custoUnitario
) {}
