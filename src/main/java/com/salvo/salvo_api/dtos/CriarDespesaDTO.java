package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CriarDespesaDTO(
    Long fornecedorId,
    @NotBlank(message = "Descrição é obrigatória") String descricao,
    String categoria,
    @NotNull(message = "Valor é obrigatório")
    @Positive(message = "Valor deve ser maior que zero") BigDecimal valor,
    String status,
    LocalDate vencimento,
    OffsetDateTime pagoEm,
    String observacoes
) {}
