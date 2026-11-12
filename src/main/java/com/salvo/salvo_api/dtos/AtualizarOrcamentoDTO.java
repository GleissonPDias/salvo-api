package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AtualizarOrcamentoDTO(
    Long clienteId,
    Long veiculoId,
    String titulo,
    String tipo,
    String observacoes,
    LocalDate validade,
    Short validadeDias,
    BigDecimal desconto,
    String descontoTipo,
    String canalEnvio,
    String status,
    List<CriarOrcamentoItemDTO> itens
) {}
