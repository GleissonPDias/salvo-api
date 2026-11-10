package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.entities.Agendamento;

import java.time.OffsetDateTime;

public record AgendamentoDTO(
    Long id,
    Long clienteId,
    Long veiculoId,
    Long servicoId,
    Long mecanicoId,
    Long boxId,
    Long ordemServicoId,
    String titulo,
    String descricao,
    OffsetDateTime inicio,
    OffsetDateTime fim,
    String status,
    Long criadoPor,
    OffsetDateTime criadoEm,
    OffsetDateTime atualizadoEm
) {
    public static AgendamentoDTO from(Agendamento a) {
        return new AgendamentoDTO(
            a.getId(), a.getClienteId(), a.getVeiculoId(), a.getServicoId(),
            a.getMecanicoId(), a.getBoxId(), a.getOrdemServicoId(), a.getTitulo(),
            a.getDescricao(), a.getInicio(), a.getFim(), a.getStatus(),
            a.getCriadoPor(), a.getCriadoEm(), a.getAtualizadoEm()
        );
    }
}
