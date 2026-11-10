package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.Despesa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record DespesaDTO(
    Long id,
    Long fornecedorId,
    String descricao,
    String categoria,
    BigDecimal valor,
    String status,
    LocalDate vencimento,
    OffsetDateTime pagoEm,
    String observacoes,
    Long criadoPor,
    OffsetDateTime criadoEm,
    OffsetDateTime atualizadoEm
) {
    public static DespesaDTO from(Despesa d) {
        return new DespesaDTO(
            d.getId(), d.getFornecedorId(), d.getDescricao(), d.getCategoria(),
            d.getValor(), d.getStatus(), d.getVencimento(), d.getPagoEm(),
            d.getObservacoes(), d.getCriadoPor(), d.getCriadoEm(), d.getAtualizadoEm()
        );
    }
}
