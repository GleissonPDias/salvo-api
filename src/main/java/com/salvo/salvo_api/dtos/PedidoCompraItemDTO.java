package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.PedidoCompraItem;

import java.math.BigDecimal;

public record PedidoCompraItemDTO(
    Long id,
    Long pecaId,
    String descricao,
    BigDecimal quantidade,
    BigDecimal custoUnitario,
    BigDecimal quantidadeRecebida,
    BigDecimal total
) {
    public static PedidoCompraItemDTO from(PedidoCompraItem item) {
        BigDecimal total = item.getQuantidade() == null || item.getCustoUnitario() == null
            ? BigDecimal.ZERO
            : item.getQuantidade().multiply(item.getCustoUnitario());
        return new PedidoCompraItemDTO(
            item.getId(), item.getPecaId(), item.getDescricao(), item.getQuantidade(),
            item.getCustoUnitario(), item.getQuantidadeRecebida(), total
        );
    }
}
