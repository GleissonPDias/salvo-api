package com.salvo.salvo_api.dtos;

import com.salvo.salvo_api.domain.os.PrioridadeOs;
import com.salvo.salvo_api.entities.OrdemServico;

import java.time.OffsetDateTime;

public record OrdemServicoResumoDTO(
    Long id,
    Integer numero,
    String status,
    PrioridadeOs prioridade,
    String clienteNome,
    String veiculoPlaca,
    String veiculoModelo,
    OffsetDateTime abertaEm
) {
    public static OrdemServicoResumoDTO from(OrdemServico os) {
        return new OrdemServicoResumoDTO(
            os.getId(),
            os.getNumero(),
            os.getStatus().getNome(),
            os.getPrioridade(),
            os.getCliente().getNome(),
            os.getVeiculo().getPlaca(),
            os.getVeiculo().getModelo(),
            os.getAbertaEm()
        );
    }
}
