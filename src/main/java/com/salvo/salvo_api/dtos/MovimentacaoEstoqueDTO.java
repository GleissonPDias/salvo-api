package com.salvo.salvo_api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Movimentação manual de estoque.
 *
 * tipo:
 *   entrada  -> soma (compra)
 *   devolucao-> soma (devolução)
 *   saida    -> subtrai
 *   ajuste   -> quantidade é a NOVA contagem física (inventário)
 */
public record MovimentacaoEstoqueDTO(
    @NotBlank(message = "Tipo da movimentação é obrigatório") String tipo,
    @NotNull(message = "Quantidade é obrigatória") @PositiveOrZero BigDecimal quantidade,
    @PositiveOrZero BigDecimal custoUnitario,
    Long fornecedorId,
    Long pedidoCompraId,
    String documento,
    String observacao
) {}
