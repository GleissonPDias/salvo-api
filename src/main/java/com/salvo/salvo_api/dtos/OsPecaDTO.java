package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.OsPeca;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OsPecaDTO(
    Long id,
    Long pecaId,
    String descricao,
    BigDecimal quantidade,
    BigDecimal valorUnitario,
    BigDecimal subtotal,
    Boolean cotada,
    Boolean instalada,
    OffsetDateTime instaladaEm,
    BigDecimal desconto,
    String descontoTipo,
    String unidade
) {
    public static OsPecaDTO from(OsPeca p) {
        return new OsPecaDTO(
            p.getId(),
            p.getPeca() != null ? p.getPeca().getId() : null,
            p.getDescricao(),
            p.getQuantidade(),
            p.getValorUnitario(),
            p.getQuantidade().multiply(p.getValorUnitario()),
            p.getCotada(),
            p.getInstalada(),
            p.getInstaladaEm(),
            p.getDesconto(),
            p.getDescontoTipo(),
            p.getUnidade()
        );
    }
}
