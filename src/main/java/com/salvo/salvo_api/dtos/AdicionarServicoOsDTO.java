package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record AdicionarServicoOsDTO(
    Long servicoId,
    @NotBlank(message = "Descrição é obrigatória") String descricao,
    @NotNull @Positive BigDecimal quantidade,
    @NotNull @PositiveOrZero BigDecimal valorUnitario,
    Long mecanicoId,
    BigDecimal tempoEstimadoHoras
) {}
