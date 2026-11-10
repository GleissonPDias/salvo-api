package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AtualizarDespesaDTO(
    Long fornecedorId,
    String descricao,
    String categoria,
    @Positive(message = "Valor deve ser maior que zero") BigDecimal valor,
    String status,
    LocalDate vencimento,
    OffsetDateTime pagoEm,
    String observacoes
) {}
