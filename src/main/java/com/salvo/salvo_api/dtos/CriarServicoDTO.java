package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CriarServicoDTO(
    String codigo,
    @NotBlank(message = "Nome do serviço é obrigatório") String nome,
    String descricao,
    String categoria,
    String detalhe,
    @PositiveOrZero(message = "Tempo estimado não pode ser negativo") BigDecimal tempoEstimadoHoras,
    String garantiaTipo,
    @PositiveOrZero(message = "Preço base não pode ser negativo") BigDecimal precoBase,
    Boolean ativo
) {}
