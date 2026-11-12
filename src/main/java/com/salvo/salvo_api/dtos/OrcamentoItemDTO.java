package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.OrcamentoItem;

import java.math.BigDecimal;

public record OrcamentoItemDTO(
    Long id,
    String tipo,
    Long pecaId,
    Long servicoId,
    String codigo,
    String descricao,
    BigDecimal quantidade,
    BigDecimal valorUnitario
) {
    public static OrcamentoItemDTO from(OrcamentoItem item) {
        return new OrcamentoItemDTO(
            item.getId(),
            item.getTipo().name(),
            item.getPecaId(),
            item.getServicoId(),
            item.getCodigo(),
            item.getDescricao(),
            item.getQuantidade(),
            item.getValorUnitario()
        );
    }
}
