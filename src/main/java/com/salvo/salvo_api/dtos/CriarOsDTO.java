package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.domain.os.PrioridadeOs;
import jakarta.validation.constraints.NotNull;

public record CriarOsDTO(
    @NotNull(message = "Cliente é obrigatório") Long clienteId,
    @NotNull(message = "Veículo é obrigatório") Long veiculoId,
    Long boxId,
    Long mecanicoLiderId,
    PrioridadeOs prioridade,
    Integer kmEntrada,
    String diagnostico,
    String observacoes
) {}
