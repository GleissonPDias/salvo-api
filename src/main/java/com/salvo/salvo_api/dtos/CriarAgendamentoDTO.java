package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record CriarAgendamentoDTO(
    Long clienteId,
    Long veiculoId,
    Long servicoId,
    Long mecanicoId,
    Long boxId,
    Long ordemServicoId,
    @NotBlank(message = "Título é obrigatório") String titulo,
    String descricao,
    @NotNull(message = "Início é obrigatório") OffsetDateTime inicio,
    @NotNull(message = "Fim é obrigatório") OffsetDateTime fim,
    String status
) {}
