package com.salvo.salvo_api.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MovimentacaoEstoqueRespostaDTO(
    Long id,
    Long pecaId,
    String pecaCodigo,
    String tipo,
    BigDecimal quantidade,
    Short direcao,
    BigDecimal saldoAnterior,
    BigDecimal saldoPosterior,
    BigDecimal saldoAtual,
    BigDecimal custoUnitario,
    String documento,
    String observacao,
    OffsetDateTime criadoEm
) {}
