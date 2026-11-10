package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.domain.os.PrioridadeOs;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record CriarOsDTO(
    @NotNull(message = "Cliente é obrigatório") Long clienteId,
    @NotNull(message = "Veículo é obrigatório") Long veiculoId,
    Long boxId,
    Long mecanicoLiderId,
    PrioridadeOs prioridade,
    Integer kmEntrada,
    String diagnostico,
    String observacoes,
    String tipo,
    String canal,
    String titulo,
    String relatoCliente,
    String observacoesInternas,
    OffsetDateTime previsaoEntrega,
    @Min(value = 0, message = "Nível de combustível deve estar entre 0 e 4")
    @Max(value = 4, message = "Nível de combustível deve estar entre 0 e 4")
    Short nivelCombustivel
) {}
