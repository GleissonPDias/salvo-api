package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.OsServico;
import java.math.BigDecimal;

public record OsServicoDTO(
    Long id,
    Long servicoId,
    Long mecanicoId,
    String mecanicoNome,
    String descricao,
    BigDecimal quantidade,
    BigDecimal valorUnitario,
    BigDecimal subtotal,
    BigDecimal tempoEstimadoHoras
) {
    public static OsServicoDTO from(OsServico s) {
        return new OsServicoDTO(
            s.getId(),
            s.getServico() != null ? s.getServico().getId() : null,
            s.getMecanico() != null ? s.getMecanico().getId() : null,
            s.getMecanico() != null ? s.getMecanico().getNome() : null,
            s.getDescricao(),
            s.getQuantidade(),
            s.getValorUnitario(),
            s.getQuantidade().multiply(s.getValorUnitario()),
            s.getTempoEstimadoHoras()
        );
    }
}
