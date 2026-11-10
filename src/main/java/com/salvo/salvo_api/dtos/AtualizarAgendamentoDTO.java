package com.salvo.salvo_api.dtos;

import java.time.OffsetDateTime;

public record AtualizarAgendamentoDTO(
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
    String status
) {}
