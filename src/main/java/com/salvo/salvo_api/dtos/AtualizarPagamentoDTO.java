package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AtualizarPagamentoDTO(
    Long ordemServicoId,
    Long orcamentoId,
    Long clienteId,
    String descricao,
    @Positive(message = "Valor deve ser maior que zero") BigDecimal valor,
    String formaPagamento,
    @Positive(message = "Parcelas deve ser maior que zero") Short parcelas,
    String status,
    LocalDate vencimento,
    OffsetDateTime pagoEm,
    String observacoes
) {}
