package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.domain.os.PrioridadeOs;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Atualização dos campos do cabeçalho da OS.
 *
 * É um PUT: os campos enviados substituem os atuais (inclusive null para
 * limpar box/mecânico/previsão). Status e itens têm endpoints próprios.
 */
public record AtualizarOsDTO(
    Long boxId,
    Long mecanicoLiderId,
    PrioridadeOs prioridade,
    Integer kmEntrada,
    @PositiveOrZero(message = "Km de saída não pode ser negativo") Integer kmSaida,
    String diagnostico,
    Boolean diagnosticoValidado,
    String observacoes,
    String observacoesInternas,
    String observacoesCliente,
    String tipo,
    String canal,
    String titulo,
    String relatoCliente,
    OffsetDateTime previsaoEntrega,
    @Min(value = 0, message = "Nível de combustível deve estar entre 0 e 4")
    @Max(value = 4, message = "Nível de combustível deve estar entre 0 e 4")
    Short nivelCombustivel,
    @PositiveOrZero(message = "Desconto não pode ser negativo") BigDecimal desconto,
    String descontoTipo,
    String formaPagamento,
    @Min(value = 1, message = "Parcelas deve ser no mínimo 1") Short parcelas,
    String garantiaTipo,
    @PositiveOrZero(message = "Garantia em dias não pode ser negativa") Integer garantiaDias,
    @PositiveOrZero(message = "Garantia em km não pode ser negativa") Integer garantiaKm,
    String aprovacaoStatus,
    String aprovacaoCanal,
    OffsetDateTime aprovacaoEnviadaEm,
    OffsetDateTime aprovacaoDecididaEm,
    OffsetDateTime aprovacaoVisualizadaEm,
    String motivoCancelamento
) {}
