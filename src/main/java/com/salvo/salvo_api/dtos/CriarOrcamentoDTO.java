package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CriarOrcamentoDTO(
    @NotNull(message = "Cliente é obrigatório")
    Long clienteId,
    @NotNull(message = "Veículo é obrigatório")
    Long veiculoId,
    String titulo,
    String tipo,
    String observacoes,
    LocalDate validade,
    Short validadeDias,
    BigDecimal desconto,
    String descontoTipo,
    String canalEnvio,
    List<CriarOrcamentoItemDTO> itens
) {}
