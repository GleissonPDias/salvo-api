package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record AtualizarPecaDTO(
    String codigo,
    String nome,
    String descricao,
    String fabricante,
    String oem,
    String categoria,
    String aplicacao,
    String unidade,
    @PositiveOrZero(message = "Preço de custo não pode ser negativo") BigDecimal precoCusto,
    @PositiveOrZero(message = "Preço de venda não pode ser negativo") BigDecimal precoVenda,
    @PositiveOrZero(message = "Estoque mínimo não pode ser negativo") BigDecimal estoqueMinimo,
    @PositiveOrZero(message = "Estoque máximo não pode ser negativo") BigDecimal estoqueMaximo,
    String localizacao,
    Long fornecedorId,
    String observacao,
    String codigoBarras,
    @PositiveOrZero(message = "Garantia não pode ser negativa") Integer garantiaDias,
    String ncm,
    Boolean ativo
) {}
