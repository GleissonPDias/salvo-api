package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.OsPeca;
import java.math.BigDecimal;

public record OsPecaDTO(
    Long id,
    Long pecaId,
    String descricao,
    BigDecimal quantidade,
    BigDecimal valorUnitario,
    BigDecimal subtotal
) {
    public static OsPecaDTO from(OsPeca p) {
        return new OsPecaDTO(
            p.getId(),
            p.getPeca() != null ? p.getPeca().getId() : null,
            p.getDescricao(),
            p.getQuantidade(),
            p.getValorUnitario(),
            p.getQuantidade().multiply(p.getValorUnitario())
        );
    }
}
