package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CriarPecaDTO(
    String codigo,
    @NotBlank(message = "Nome da peça é obrigatório") String nome,
    String descricao,
    String fabricante,
    String oem,
    String categoria,
    String aplicacao,
    String unidade,
    @PositiveOrZero(message = "Preço de custo não pode ser negativo") BigDecimal precoCusto,
    @PositiveOrZero(message = "Preço de venda não pode ser negativo") BigDecimal precoVenda,
    @PositiveOrZero(message = "Estoque inicial não pode ser negativo") BigDecimal estoqueAtual,
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
