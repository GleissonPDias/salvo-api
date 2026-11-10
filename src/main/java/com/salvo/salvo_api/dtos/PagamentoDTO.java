package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.Pagamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PagamentoDTO(
    Long id,
    Long ordemServicoId,
    Long orcamentoId,
    Long clienteId,
    String descricao,
    BigDecimal valor,
    String formaPagamento,
    Short parcelas,
    String status,
    LocalDate vencimento,
    OffsetDateTime pagoEm,
    String observacoes,
    Long criadoPor,
    OffsetDateTime criadoEm,
    OffsetDateTime atualizadoEm
) {
    public static PagamentoDTO from(Pagamento p) {
        return new PagamentoDTO(
            p.getId(), p.getOrdemServicoId(), p.getOrcamentoId(), p.getClienteId(),
            p.getDescricao(), p.getValor(), p.getFormaPagamento(), p.getParcelas(),
            p.getStatus(), p.getVencimento(), p.getPagoEm(), p.getObservacoes(),
            p.getCriadoPor(), p.getCriadoEm(), p.getAtualizadoEm()
        );
    }
}
